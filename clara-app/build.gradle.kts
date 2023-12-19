import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import org.gradle.jvm.tasks.Jar
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.docker)
    alias(libs.plugins.protobuf)
    application
}

group = "de.unistuttgart.iste.sqa.clara"
version = file("version.txt")
    .readText()
    .trim()
    .also { specifiedVersion ->
        require(specifiedVersion.matches(Regex("""^\d+\.\d+\.\d+$"""))) {
            "The version specified in 'version.txt' must be formatted as semver, like '1.4.3'. Specified value: '$specifiedVersion'"
        }
    }

val dockerImageName = "ghcr.io/stevebinary/${project.name}"

val buildInformation = BuildInformation(project)

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.bundles.logging)
    implementation(libs.bundles.configuration)
    implementation(libs.arrow.core)
    implementation(libs.fabric8.kubernetes.client)
    implementation(libs.bundles.grpc)
    implementation(libs.opentelemetry.api)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.bundles.grpc)
    implementation(libs.opentelemetry.api)

    testImplementation(libs.bundles.kotest)
}

application {
    mainClass = "de.unistuttgart.iste.sqa.clara.MainKt"
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    dependsOn(generateBuildInformation)

    sourceSets.main {
        resources {
            // automatically add the generated build information to the resources of the artefact
            srcDir(generateBuildInformation.destinationFile.get().asFile.parentFile)
        }
    }
}

val generateBuildInformation by tasks.creating(WriteProperties::class) {
    group = "build"
    description = "Generate the properties file containing information about the build."

    property("version", buildInformation.appVersion)
    property("build-time", buildInformation.buildTime)
    property("git-branch", buildInformation.gitBranchString)
    property("git-commit", buildInformation.gitCommit)

    destinationFile = project.layout.buildDirectory.get().dir("build-information").file("build-information.properties")
}

val standaloneJar by tasks.creating(Jar::class) {
    dependsOn(tasks.classes)

    group = "build"
    description = "Build a standalone jar which contains all dependencies, also known as a 'fat jar'."
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveClassifier = "standalone"

    manifest {
        attributes["Main-Class"] = application.mainClass.get()
        attributes["Implementation-Title"] = project.name
        attributes["Implementation-Description"] = "Cloud Architecture Recovery Assistant"
        attributes["Implementation-Version"] = project.version.toString()
        attributes["Implementation-Build-Date"] = buildInformation.buildTime
        attributes["Created-By"] = "Gradle ${gradle.gradleVersion}"
        attributes["Build-Git-Branch"] = buildInformation.gitBranchString
        attributes["Build-Git-Commit"] = buildInformation.gitCommit
        attributes["Build-Jdk"] = "${System.getProperty("java.version")} (${System.getProperty("java.vendor")} ${System.getProperty("java.vm.version")})"
        attributes["Sealed"] = true
    }

    from(sourceSets.main.get().output)
    from(configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) })
}

val copyStandaloneJarIntoDockerfileDirectory by tasks.creating(Copy::class) {
    dependsOn(standaloneJar)

    group = "docker"
    description = "Copy the standalone jar of the application to the directory of the Dockerfile."

    from(standaloneJar.outputs)
    into(project.layout.buildDirectory.dir("docker"))
}

val createDockerfile by tasks.creating(Dockerfile::class) {
    group = "docker"
    description = "Create the Dockerfile for the image."

    // create the minified JRE

    from("eclipse-temurin:17-alpine as jre-build-stage")

    runCommand(
        "\$JAVA_HOME/bin/jlink" +
                " --add-modules java.base" +
                " --add-modules java.desktop" +
                " --add-modules java.naming" +
                " --add-modules java.sql" +
                " --add-modules java.xml" +
                " --no-man-pages" +
                " --no-header-files" +
                " --strip-debug" +
                " --compress=2" +
                " --output /jre"
    )

    // create the actual image

    from("alpine:3.19")

    arg("CREATION_TIME")

    label(
        mapOf(
            "org.opencontainers.image.title" to project.name,
            "org.opencontainers.image.description" to "Cloud Architecture Recovery Assistant",
            "org.opencontainers.image.created" to "\$CREATION_TIME",
            "org.opencontainers.image.version" to buildInformation.appVersion,
            "org.opencontainers.image.revision" to buildInformation.gitCommit,
            "org.opencontainers.image.source" to "https://github.com/SteveBinary/clara",
            "org.opencontainers.image.url" to "https://github.com/SteveBinary/clara",
        )
    )

    runCommand("addgroup --system --gid 1337 clara")
    runCommand("adduser --disabled-password --no-create-home --system --uid 1337 --ingroup clara clara")
    runCommand("apk add graphviz")

    workingDir("/app")
    runCommand("chown -R clara:clara /app")

    copyFile("--chown=clara:clara --from=jre-build-stage /jre", "/jre")
    copyFile("--chown=clara:clara ${project.name}-${project.version}-${standaloneJar.archiveClassifier.get()}.jar", "/app/${project.name}.jar")

    user("clara")
    defaultCommand("/jre/bin/java", "-jar", "${project.name}.jar")
}

val buildImage by tasks.creating(DockerBuildImage::class) {
    dependsOn(createDockerfile, copyStandaloneJarIntoDockerfileDirectory)

    group = "docker"
    description = "Build the CLARA Docker image."

    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    val creationTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX")
    val creationTime = creationTimeFormat.format(Date())

    buildArgs = mapOf("CREATION_TIME" to creationTime)

    images = dockerImages()
}

val pushImage by tasks.creating(DockerPushImage::class) {
    dependsOn(buildImage)

    group = "docker"
    description = "Push the CLARA Docker image to the container registry."

    images = dockerImages()
}

fun dockerImages(): List<String> {
    val gitBranch = buildInformation.gitBranch
    val dockerImageTag = when (gitBranch) {
        is GitUtils.Branch.Main -> "v${project.version}"
        is GitUtils.Branch.None -> "v${project.version}-unknown"
        is GitUtils.Branch.Other -> "v${project.version}-${gitBranch.name}"
    }

    return buildList {
        add("$dockerImageName:$dockerImageTag")

        if (gitBranch == GitUtils.Branch.Main) {
            add("$dockerImageName:latest")
        }
    }
}

class BuildInformation(project: Project) {

    private val git = GitUtils(project)

    val appVersion = project.version.toString()
    val gitCommit = git.currentCommitId()
    val gitBranch = git.currentBranch()

    val buildTime: String = let {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        val buildTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX")

        buildTimeFormat.format(Date())
    }

    val gitBranchString = when (gitBranch) {
        is GitUtils.Branch.Main -> "main"
        is GitUtils.Branch.None -> "<unknown>"
        is GitUtils.Branch.Other -> gitBranch.name
    }
}

class GitUtils(private val project: Project) {

    sealed interface Branch {

        object Main : Branch

        object None : Branch

        @JvmInline
        value class Other(val name: String) : Branch
    }

    fun currentBranch(): Branch {
        val branch = try {
            val gitProcessOutput = ByteArrayOutputStream()

            project.exec {
                commandLine = listOf("git", "rev-parse", "--abbrev-ref", "HEAD")
                standardOutput = gitProcessOutput
            }

            String(gitProcessOutput.toByteArray()).trim()
        } catch (ex: Exception) {
            project.logger.warn("Unable to determine current branch: ${ex.message}")
            return Branch.None
        }

        if (branch == "HEAD") {
            project.logger.warn("Unable to determine current branch: Project is checked out with detached head!")
            return Branch.None
        }

        if (branch == "main") {
            return Branch.Main
        }

        return Branch.Other(branch)
    }

    fun currentCommitId(): String {
        try {
            val gitProcessOutput = ByteArrayOutputStream()

            project.exec {
                commandLine = listOf("git", "rev-parse", "HEAD")
                standardOutput = gitProcessOutput
            }

            return String(gitProcessOutput.toByteArray()).trim()
        } catch (ex: Exception) {
            project.logger.warn("Unable to determine current git commit ID: ${ex.message}")
            return "<unknown>"
        }
    }
}

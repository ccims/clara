import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import org.gradle.jvm.tasks.Jar
import java.io.ByteArrayOutputStream

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.docker)
    alias(libs.plugins.protobuf)
    application
}

group = "de.unistuttgart.iste.sqa.clara"
version = file("version.txt").readText().trim()

val dockerImageName = "ghcr.io/stevebinary/${rootProject.name}"

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

    testImplementation(libs.bundles.kotest)
}

application {
    mainClass = "de.unistuttgart.iste.sqa.clara.MainKt"
}

tasks.test {
    useJUnitPlatform()
}

val standaloneJar by tasks.creating(Jar::class) {
    dependsOn("compileJava", "compileKotlin", "processResources")

    group = "build"
    description = "Build a standalone jar which contains all dependencies, also known as a 'fat jar'."
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveClassifier = "standalone"

    manifest {
        attributes["Main-Class"] = application.mainClass
    }

    from(sourceSets.main.get().output)
    from(configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) })
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

val copyStandaloneJarIntoDockerfileDirectory by tasks.creating(Copy::class) {
    dependsOn(standaloneJar)

    group = "docker"
    description = "Copy the standalone jar of the application to the directory of the Dockerfile."

    from(standaloneJar.outputs)
    into(project.layout.buildDirectory.dir("docker"))
}

val buildImage by tasks.creating(DockerBuildImage::class) {
    dependsOn(createDockerfile, copyStandaloneJarIntoDockerfileDirectory)

    group = "docker"
    description = "Build the CLARA Docker image."

    images = dockerImages()
}

val pushImage by tasks.creating(DockerPushImage::class) {
    dependsOn(buildImage)

    group = "docker"
    description = "Push the CLARA Docker image to the container registry."

    images = dockerImages()
}

fun dockerImages(): List<String> {
    val gitBranch = gitBranch()
    val dockerImageTag = dockerImageTagFromGitBranchAndVersion(gitBranch)

    return buildList {
        add("$dockerImageName:$dockerImageTag")

        if (gitBranch == GitBranch.Main) {
            add("$dockerImageName:latest")
        }
    }
}

fun dockerImageTagFromGitBranchAndVersion(branch: GitBranch): String {
    return when (branch) {
        is GitBranch.Main -> "v${project.version}"
        is GitBranch.None -> "v${project.version}-unknown"
        is GitBranch.Other -> "v${project.version}-${branch.name}"
    }
}

fun gitBranch(): GitBranch {
    val branch = try {
        val gitProcessOutput = ByteArrayOutputStream()

        exec {
            commandLine = listOf("git", "rev-parse", "--abbrev-ref", "HEAD")
            standardOutput = gitProcessOutput
        }

        String(gitProcessOutput.toByteArray()).trim()
    } catch (ex: Exception) {
        logger.warn("Unable to determine current branch: ${ex.message}")
        return GitBranch.None
    }

    if (branch == "HEAD") {
        logger.warn("Unable to determine current branch: Project is checked out with detached head!")
        return GitBranch.None
    }

    if (branch == "main") {
        return GitBranch.Main
    }

    return GitBranch.Other(branch)
}

sealed interface GitBranch {

    object Main : GitBranch

    object None : GitBranch

    @JvmInline
    value class Other(val name: String) : GitBranch
}

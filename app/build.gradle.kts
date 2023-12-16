import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import org.gradle.jvm.tasks.Jar

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.docker)
    application
}

group = "de.unistuttgart.iste.sqa.clara"
version = file("version.txt").readText().trim()

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.bundles.logging)
    implementation(libs.bundles.configuration)
    implementation(libs.arrow.core)
    implementation(libs.fabric8.kubernetes.client)
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
    description = "Build a standalone jar which contains all dependencies, also known as a 'fat jar'"
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
    description = "Create the Dockerfile for the image"

    // create the minified JRE

    from("eclipse-temurin:17-alpine as jre-build-stage")

    runCommand(
        "\$JAVA_HOME/bin/jlink" +
                " --add-modules java.base" +
                " --add-modules java.xml" +
                " --add-modules java.naming" +
                " --add-modules java.sql" +
                " --add-modules java.desktop" +
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
    user("clara")

    copyFile("--chown=clara:clara --from=jre-build-stage /jre", "/jre")
    copyFile("--chown=clara:clara ${project.name}-${project.version}-${standaloneJar.archiveClassifier.get()}.jar", "/app/${project.name}.jar")

    defaultCommand("/jre/bin/java", "-jar", "${project.name}.jar")
}

val copyStandaloneJarIntoDirectoryOfDockerfile by tasks.creating(Copy::class) {
    from(standaloneJar.outputs)
    into(project.layout.buildDirectory.dir("docker"))
}


val buildImage by tasks.creating(DockerBuildImage::class) {
    dependsOn(createDockerfile, copyStandaloneJarIntoDirectoryOfDockerfile)

    group = "docker"
    description = "Build the CLARA Docker image"

    images = listOf("ghcr.io/stevebinary/${rootProject.name}:${project.version}")
}

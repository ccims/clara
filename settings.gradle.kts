plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs.
    // This cannot come from the version catalog because it itself is defined in this file.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "clara"

include("clara-app")
include("clara-integration-tests")

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS

    repositories {
        mavenCentral()
    }

    versionCatalogs {
        create("libs") {
            declaredGradlePlugins()
            declaredLibraries()
        }
    }
}

fun VersionCatalogBuilder.declaredGradlePlugins() {
    val gradlePluginKotlinVersion = version("gradle-plugin-kotlin", "1.9.22")
    val gradlePluginDockerVersion = version("gradle-docker-plugin", "9.4.0")
    val gradlePluginProtobufVersion = version("gradle-protobuf-plugin", "0.9.4")

    plugin("kotlin", "org.jetbrains.kotlin.jvm").versionRef(gradlePluginKotlinVersion)
    plugin("docker", "com.bmuschko.docker-remote-api").versionRef(gradlePluginDockerVersion)
    plugin("protobuf", "com.google.protobuf").versionRef(gradlePluginProtobufVersion)
}

fun VersionCatalogBuilder.declaredLibraries() {
    val hopliteVersion = version("hoplite", "2.7.5")
    val arrowVersion = version("arrow", "1.2.1")
    val kotlinLoggingVersion = version("kotlin-logging", "6.0.1")
    val logbackClassicVersion = version("logback-classic", "1.4.14")
    val fabric8Version = version("fabric8", "6.10.0")
    val kotestVersion = version("kotest", "5.8.0")
    val kotlinxCoroutineVersion = version("kotlinx-coroutine", "1.8.0-RC2") // TODO: needed because of the support for Kotlin 1.9.22; update to stable when available
    val opentelemetryVersion = version("opentelemetry", "1.34.0")

    val grpcVersion = version("grpc", "1.60.1")
    val grpcKotlinVersion = version("grpc-kotlin", "1.4.1")
    val protobufVersion = version("protobuf-kotlin", "3.25.2")

    library("hoplite.core", "com.sksamuel.hoplite", "hoplite-core").versionRef(hopliteVersion)
    library("hoplite.yaml", "com.sksamuel.hoplite", "hoplite-yaml").versionRef(hopliteVersion)

    library("arrow.core", "io.arrow-kt", "arrow-core").versionRef(arrowVersion)

    library("kotlin.logging.jvm", "io.github.oshai", "kotlin-logging-jvm").versionRef(kotlinLoggingVersion)
    library("logback.classic", "ch.qos.logback", "logback-classic").versionRef(logbackClassicVersion)

    library("fabric8.kubernetes.client", "io.fabric8", "kubernetes-client").versionRef(fabric8Version)

    library("kotest.runner.junit5", "io.kotest", "kotest-runner-junit5").versionRef(kotestVersion)
    library("kotest.assertions.core", "io.kotest", "kotest-assertions-core").versionRef(kotestVersion)
    library("kotest.framework.datatest", "io.kotest", "kotest-framework-datatest").versionRef(kotestVersion)
    library("kotest.property", "io.kotest", "kotest-property").versionRef(kotestVersion)

    library("grpc.protobuf", "io.grpc", "grpc-protobuf").versionRef(grpcVersion)
    library("grpc.netty", "io.grpc", "grpc-netty").versionRef(grpcVersion)
    library("grpc.kotlin.stub", "io.grpc", "grpc-kotlin-stub").versionRef(grpcKotlinVersion)
    library("grpc.kotlin.protocGenGrpcKotlin", "io.grpc", "protoc-gen-grpc-kotlin").version("1.4.1:jdk8@jar") // Update version when grpcKotlinVersion changes! Gradle doesn't understand this syntax when specified as above.
    library("grpc.java.protocGenGrpcJava", "io.grpc", "protoc-gen-grpc-java").versionRef(grpcVersion)

    library("protobuf.protoc", "com.google.protobuf", "protoc").versionRef(protobufVersion)
    library("protobuf.kotlin", "com.google.protobuf", "protobuf-kotlin").versionRef(protobufVersion)

    library("opentelemetry.api", "io.opentelemetry", "opentelemetry-api").versionRef(opentelemetryVersion)

    library("kotlinx.coroutines.core", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").versionRef(kotlinxCoroutineVersion)

    bundle("configuration", listOf("hoplite.core", "hoplite.yaml"))
    bundle("logging", listOf("kotlin-logging-jvm", "logback.classic"))
    bundle("kotest", listOf("kotest.runner.junit5", "kotest.assertions.core", "kotest.framework.datatest", "kotest.property"))
    bundle("grpc", listOf("grpc.protobuf", "protobuf.kotlin", "grpc.kotlin.stub", "grpc.netty"))
}
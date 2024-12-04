plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs.
    // This cannot come from the version catalog because it itself is defined in this file.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
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
    val gradlePluginKotlinVersion = version("gradle-kotlin-plugin", "2.1.0")
    val gradlePluginDockerVersion = version("gradle-docker-plugin", "9.4.0")
    val gradlePluginProtobufVersion = version("gradle-protobuf-plugin", "0.9.4")
    val gradlePluginGraphQLVersion = version("gradle-graphql-plugin", "8.2.1")

    plugin("kotlin", "org.jetbrains.kotlin.jvm").versionRef(gradlePluginKotlinVersion)
    plugin("docker", "com.bmuschko.docker-remote-api").versionRef(gradlePluginDockerVersion)
    plugin("protobuf", "com.google.protobuf").versionRef(gradlePluginProtobufVersion)
    plugin("graphql", "com.expediagroup.graphql").versionRef(gradlePluginGraphQLVersion)
}

fun VersionCatalogBuilder.declaredLibraries() {
    val hopliteVersion = version("hoplite", "2.9.0")
    val arrowVersion = version("arrow", "2.0.0-rc.1")
    val kotlinLoggingVersion = version("kotlin-logging", "7.0.3")
    val logbackClassicVersion = version("logback-classic", "1.5.12")
    val fabric8Version = version("fabric8", "7.0.0")
    val kotestVersion = version("kotest", "5.9.1")
    val kotlinxCoroutineVersion = version("kotlinx-coroutine", "1.9.0")
    val opentelemetryVersion = version("opentelemetry", "1.44.1")
    val graphQLVersion = version("graphql", "8.2.1")

    val grpcVersion = version("grpc", "1.68.2")
    val grpcKotlinVersion = version("grpc-kotlin", "1.4.1")
    val protobufVersion = version("protobuf-kotlin", "4.29.0")

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

    library("graphql.client", "com.expediagroup", "graphql-kotlin-spring-client").versionRef(graphQLVersion)

    bundle("logging", listOf("kotlin-logging-jvm", "logback.classic"))
    bundle("kotest", listOf("kotest.runner.junit5", "kotest.assertions.core", "kotest.framework.datatest", "kotest.property"))
    bundle("grpc", listOf("grpc.protobuf", "protobuf.kotlin", "grpc.kotlin.stub", "grpc.netty"))
}

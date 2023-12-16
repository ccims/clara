plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs.
    // This cannot come from the version catalog because it itself is defined in this file.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "clara"

include("app")

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
    val gradlePluginKotlinVersion = version("gradle-plugin-kotlin", "1.9.21")
    val dockerPluginVersion = version("docker-plugin", "9.4.0")

    plugin("kotlin", "org.jetbrains.kotlin.jvm").versionRef(gradlePluginKotlinVersion)
    plugin("docker", "com.bmuschko.docker-remote-api").versionRef(dockerPluginVersion)
}

fun VersionCatalogBuilder.declaredLibraries() {
    val hopliteVersion = version("hoplite", "2.7.5")
    val arrowVersion = version("arrow", "1.2.1")
    val kotlinLoggingVersion = version("kotlin-logging", "5.1.0")
    val logbackClassicVersion = version("logback-classic", "1.4.11")
    val fabric8Version = version("fabric8", "6.9.2")
    val kotestVersion = version("kotest", "5.8.0")
    val kotlinxCoroutineVersion = version("kotlinx-coroutine", "1.8.0-RC") // TODO: needed because of the support for Kotlin 1.9.21; update to stable when available

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

    library("kotlinx.coroutines.core", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").versionRef(kotlinxCoroutineVersion)

    bundle("configuration", listOf("hoplite.core", "hoplite.yaml"))
    bundle("logging", listOf("kotlin-logging-jvm", "logback.classic"))
    bundle("kotest", listOf("kotest.runner.junit5", "kotest.assertions.core", "kotest.framework.datatest", "kotest.property"))
}

plugins {
    alias(libs.plugins.kotlin)
    application
}

group = "de.unistuttgart.iste.sqa.clara"

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(project(":clara-app"))

    testImplementation(libs.bundles.kotest)

    testImplementation(libs.grpc.kotlin.stub)
    testImplementation(libs.protobuf.kotlin)
    testImplementation(libs.arrow.core)
}

tasks.test {
    useJUnitPlatform()
}

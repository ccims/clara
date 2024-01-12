plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.protobuf)
    application
}

group = "de.unistuttgart.iste.sqa.clara"

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.bundles.kotest)
}

tasks.test {
    useJUnitPlatform()
}

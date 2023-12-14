plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.protobuf)
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
    implementation(libs.bundles.grpc)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.opentelemetry.api)

    testImplementation(libs.bundles.kotest)
}

application {
    mainClass = "de.unistuttgart.iste.sqa.clara.MainKt"
}

tasks.test {
    useJUnitPlatform()
}

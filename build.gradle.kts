plugins {
    kotlin("jvm") version "1.9.23"
    application
    alias(libs.plugins.serialization)
}

group = "org.cryptobiotic"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlinx.cli )
    implementation(libs.ktor.serialization.kotlinx.json.jvm )
    implementation(libs.bundles.logging)
    implementation("ch.obermuhlner:big-math:2.3.2")
    implementation("org.apache.commons:commons-text:1.10.0") // TODO remove
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
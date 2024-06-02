plugins {
    kotlin("jvm") version "1.9.23"
}

group = "org.cryptobiotic"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
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
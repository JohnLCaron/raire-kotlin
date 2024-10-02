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

//             <groupId>com.fasterxml.jackson.core</groupId>
//            <artifactId>jackson-databind</artifactId>
//            <version>2.15.3</version>
dependencies {
    implementation(libs.kotlinx.cli )
    implementation(libs.bull.result)
    implementation(libs.ktor.serialization.kotlinx.json.jvm )
    implementation(libs.bundles.logging)
    // implementation("ch.obermuhlner:big-math:2.3.2")
    // implementation("com.fasterxml.jackson.core:jackson-databind:2.15.3")
    implementation("org.apache.commons:commons-text:1.10.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
import org.gradle.api.Project.DEFAULT_VERSION
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version embeddedKotlinVersion
    id("io.johnsonlee.sonatype-publish-plugin") version "1.9.0"
}

group = "io.johnsonlee.playground"
version = project.findProperty("version")?.takeIf { it != DEFAULT_VERSION } ?: "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

dependencies {
    api(kotlin("bom"))
    api(kotlin("stdlib"))
    api(kotlin("reflect"))
    api(libs.android.tools.build.aapt2.proto)
    api(libs.android.tools.common)
    api(libs.android.tools.external.intellij.core)
    api(libs.android.tools.layoutlib.api)
    api(libs.android.tools.sdk.common)
    api(libs.androidx.lifecycle.common.java8)
    api(libs.jackson.databind)
    api(libs.kxml2)
    api(libs.layoutlib.native.jdk11)
    api(libs.okio)
    api(libs.protobuf)
    api(libs.slf4j.api)
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

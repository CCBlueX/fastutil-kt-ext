import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    `maven-publish`
    kotlin("jvm") version "2.0.0"
    id("fastutil-ext-generator")
}

group = "moe.lasoleil"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly("it.unimi.dsi:fastutil:8.5.15")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}


tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
//        freeCompilerArgs.add("-Xsuppress=NOTHING_TO_INLINE")
    }
}

kotlin {
    jvmToolchain(8)

    sourceSets {
        main {
            kotlin.srcDir(layout.buildDirectory.dir("generated/sources"))
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}

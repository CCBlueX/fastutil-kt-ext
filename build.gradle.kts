import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    `maven-publish`
    kotlin("jvm") version "2.0.0"
    id("fastutil-ext-generator")
}

group = "net.ccbluex"
version = "0.1.0"

val projectName = project.name
val projectDescription = "Kotlin extensions for vigna's fastutil"
val authorId = "MukjepScarlet"
val authorName = "木葉 Scarlet"
val projectUrl = "https://github.com/ccbluex/$projectName"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly("it.unimi.dsi:fastutil:8.5.15")

//    testImplementation(kotlin("test"))
}

//tasks.test {
//    useJUnitPlatform()
//}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}


tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
//        freeCompilerArgs.add("-Xsuppress=NOTHING_TO_INLINE")
    }
}

val generatorOutput = layout.buildDirectory.dir("generated/sources")

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    dependsOn("compileJava")
    dependsOn("compileKotlin")
    from(sourceSets.main.map { it.allSource })
}

tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

tasks.withType<Jar> {
    manifest {
        attributes(
            "Implementation-Title" to projectName,
            "Implementation-Version" to version,
            "Implementation-Vendor" to authorId,
        )
    }

    // Include LICENSE file in the JAR
    from("LICENSE") {
        into("META-INF/")
    }
}

kotlin {
    jvmToolchain(8)

    sourceSets {
        main {
            kotlin.srcDir(generatorOutput)
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])

            pom {
                name.set(projectName)
                description.set(projectDescription)
                url.set(projectUrl)

                licenses {
                    license {
                        name.set("The MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set(authorId)
                        name.set(authorName)
                        organization.set("ccbluex")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/ccbluex/$projectName.git")
                    developerConnection.set("scm:git:ssh://github.com:ccbluex/$projectName.git")
                    url.set(projectUrl)
                }
            }
        }
    }

    repositories {
        maven {
            name = "ccbluex-maven"
            url = uri("https://maven.ccbluex.net/releases")
            credentials {
                username = System.getenv("MAVEN_TOKEN_NAME")
                password = System.getenv("MAVEN_TOKEN_SECRET")
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

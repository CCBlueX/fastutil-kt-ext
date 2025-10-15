import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    `java-library`
    `maven-publish` apply true
    alias(libs.plugins.kotlin)
    alias(libs.plugins.dokka)
}

val projectName = project.name
val projectDescription = "Kotlin extensions for vigna's fastutil"
val authorId = "MukjepScarlet"
val authorName = "木葉 Scarlet"
val projectUrl = "https://github.com/ccbluex/$projectName"

allprojects {
    group = "net.ccbluex"
    version = "0.1.5"

    repositories {
        mavenLocal()
        mavenCentral()
    }
}

tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets.configureEach {
        jdkVersion.set(8)
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.dokka")

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlin {
        jvmToolchain(8)
    }

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
        from(rootProject.file("LICENSE")) {
            into("META-INF/")
        }
    }

    tasks.withType<PublishToMavenRepository> {
        dependsOn(tasks.test)
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
            mavenLocal()
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
}

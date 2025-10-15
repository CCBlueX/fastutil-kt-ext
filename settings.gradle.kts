plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "fastutil4k"

include("fastutil4k-extensions-only", "fastutil4k-more-collections")

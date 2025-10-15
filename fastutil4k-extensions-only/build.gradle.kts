plugins {
    id("fastutil-ext-generator")
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly(libs.fastutil)
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDir(fastutilGeneratorOutput)
        }
    }
}

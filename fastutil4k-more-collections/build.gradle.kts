dependencies {
    compileOnly(libs.fastutil)
    compileOnly(project(":fastutil4k-extensions-only"))

    testCompileOnly(project(":fastutil4k-extensions-only"))
    testImplementation(kotlin("test"))
    testImplementation(libs.fastutil)
}

tasks.test {
    useJUnitPlatform()
}

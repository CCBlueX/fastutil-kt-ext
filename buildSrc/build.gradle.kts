plugins {
    `kotlin-dsl`
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvmToolchain(8)
}

dependencies {
    api("com.squareup.okio:okio:3.15.0") // -> OkHttp 5.0.0
}

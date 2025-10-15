# fastutil-kt-extensions

This module only contains `inline` functions. So you can use it as `compileOnly` dependency. It won't make your jar archive larger.

Usage:

```kotlin
repositories {
    maven {
        name = "CCBlueX"
        url = uri("https://maven.ccbluex.net/releases")
    }
}

dependencies {
    compileOnly("") // TODO
}
```

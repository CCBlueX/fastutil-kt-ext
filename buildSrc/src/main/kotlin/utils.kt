import org.gradle.api.Project

internal inline fun forEachTypes(block: (type: FastutilType) -> Unit) {
    FastutilType.values().forEach(block)
}

internal inline fun forEachPrimitiveTypes(block: (type: FastutilType) -> Unit) {
    FastutilType.values().forEach {
        if (it.isGeneric) return@forEach
        block(it)
    }
}

internal inline fun forEachMapTypes(block: (left: FastutilType, right: FastutilType) -> Unit) {
    forEachTypes { left ->
        if (left == FastutilType.BOOLEAN) return@forEachTypes // BooleanMap does not exist

        forEachTypes { right ->
            block(left, right)
        }
    }
}

val Project.fastutilGeneratorOutput get() = layout.buildDirectory.dir("generated/fastutil-kt")

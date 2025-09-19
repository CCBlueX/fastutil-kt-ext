val generatedDir = layout.buildDirectory.dir("generated/sources")

tasks.named("compileKotlin") {
    dependsOn(generateAllTask)
}

val generateAllTask = tasks.register("generateAll") {
    group = TASK_GROUP
    dependsOn(
        pairComponentNTask,
        pairFactoryTask,
        immutableListFactoryTask,
        mutableListFactoryTask,
    )
}

/**
 * Example:
 * - `val (left, right) = IntCharPair.of(1, 'c')`
 */
val pairComponentNTask = tasks.register<GenerateSrcTask>("pair-componentN") {
    group = TASK_GROUP

    file.set(generatedDir.map { it.file("pairs-componentN.kt") })
    packageName.set("moe.lasoleil.fastutil.pairs")
    imports.add("it.unimi.dsi.fastutil.Pair")
    imports.addAll(IMPORT_ALL)

    content {
        appendLine("inline operator fun <K, V> Pair<K, V>.component1() = left()")
        appendLine("inline operator fun <K, V> Pair<K, V>.component2() = right()")

        FastutilType.values().forEach { left ->
            FastutilType.values().forEach { right ->
                when {
                    left.isGeneric && right.isGeneric -> if (left != FastutilType.OBJECT && right != FastutilType.OBJECT) { // ObjectObjectPair does not exist
                        appendLine("inline operator fun <K, V> ${left}${right}Pair<K, V>.component1() = left()")
                        appendLine("inline operator fun <K, V> ${left}${right}Pair<K, V>.component2() = right()")
                    }
                    left.isGeneric -> {
                        appendLine("inline operator fun <K> ${left}${right}Pair<K>.component1() = left()")
                        appendLine("inline operator fun ${left}${right}Pair<*>.component2() = right${right}()")
                    }
                    right.isGeneric -> {
                        appendLine("inline operator fun ${left}${right}Pair<*>.component1() = left${left}()")
                        appendLine("inline operator fun <V> ${left}${right}Pair<V>.component2() = right()")
                    }
                    else -> {
                        appendLine("inline operator fun ${left}${right}Pair.component1() = left${left}()")
                        appendLine("inline operator fun ${left}${right}Pair.component2() = right${right}()")
                    }
                }
            }
        }
    }
}

// TODO
val pairFactoryTask = tasks.register<GenerateSrcTask>("pair-factory") {
    group = TASK_GROUP

    file.set(generatedDir.map { it.file("pairs-factory.kt") })
    packageName.set("moe.lasoleil.fastutil.pairs")
    imports.add("it.unimi.dsi.fastutil.Pair")
    imports.addAll(IMPORT_ALL)

    content {
        appendLine("inline fun <K, V> pair(left: K, right: V): Pair<K, V> = ObjectObjectImmutablePair(left, right)")
    }
}

/**
 * Example:
 * - `intListOf(...)
 * - `intArrayOf(...).asIntList()`
 */
val immutableListFactoryTask = tasks.register<GenerateSrcTask>("immutable-list-factory") {
    group = TASK_GROUP

    file.set(generatedDir.map { it.file("immutable-list-factory.kt") })

    packageName.set("moe.lasoleil.fastutil.lists")
    imports.addAll(IMPORT_ALL)

    content {
        FastutilType.values().forEach { type ->
            if (type.isGeneric) {
                appendLine("inline fun <T> ${type.lowercaseName}ListOf(): ${type.typeName}List<T> = ${type.typeName}ImmutableList.of()")
                appendLine("inline fun <T> ${type.lowercaseName}ListOf(element: T): ${type.typeName}List<T> = ${type.typeName}Lists.singleton(element)")
                appendLine("inline fun <T> ${type.lowercaseName}ListOf(vararg elements: T): ${type.typeName}List<T> = ${type.typeName}ImmutableList(elements)")

                appendLine("inline fun <T> Array<out T>.as${type.typeName}List(): ${type.typeName}List<T> = ${type.typeName}ImmutableList(this)")
                appendLine("inline fun <T> Array<out T>.as${type.typeName}List(offset: Int = 0, length: Int = this.size): ${type.typeName}List<T> = ${type.typeName}ImmutableList(this, offset, length)")
            } else {
                appendLine("inline fun ${type.lowercaseName}ListOf(): ${type.typeName}List = ${type.typeName}ImmutableList.of()")
                appendLine("inline fun ${type.lowercaseName}ListOf(element: ${type.typeName}): ${type.typeName}List = ${type.typeName}Lists.singleton(element)")
                appendLine("inline fun ${type.lowercaseName}ListOf(vararg elements: ${type.typeName}): ${type.typeName}List = ${type.typeName}ImmutableList(elements)")

                appendLine("inline fun ${type.typeName}Array.as${type.typeName}List(): ${type.typeName}List = ${type.typeName}ImmutableList(this)")
                appendLine("inline fun ${type.typeName}Array.as${type.typeName}List(offset: Int = 0, length: Int = this.size): ${type.typeName}List = ${type.typeName}ImmutableList(this, offset, length)")
            }
        }
    }
}

/**
 * Example:
 * - `intMutableListOf(...)`
 * - `intArrayOf(...).toIntMutableList()`
 */
val mutableListFactoryTask = tasks.register<GenerateSrcTask>("mutable-list-factory") {
    group = TASK_GROUP

    file.set(generatedDir.map { it.file("mutable-list-factory.kt") })

    packageName.set("moe.lasoleil.fastutil.lists")
    imports.addAll(IMPORT_ALL)

    content {
        FastutilType.values().forEach { type ->
            if (type.isGeneric) {
                appendLine("inline fun <T> ${type.lowercaseName}MutableListOf(): ${type.typeName}List<T> = ${type.typeName}ArrayList()")
                appendLine("inline fun <T> ${type.lowercaseName}MutableListOf(vararg elements: T): ${type.typeName}List<T> = ${type.typeName}ArrayList(elements)")

                appendLine("inline fun <T> Array<out T>.to${type.typeName}MutableList(offset: Int = 0, length: Int = this.size): ${type.typeName}List<T> = ${type.typeName}ArrayList(this, offset, length)")
            } else {
                appendLine("inline fun ${type.lowercaseName}MutableListOf(): ${type.typeName}List = ${type.typeName}ArrayList()")
                appendLine("inline fun ${type.lowercaseName}MutableListOf(element: ${type.typeName}): ${type.typeName}List = ${type.typeName}ArrayList.wrap(${type.lowercaseName}ArrayOf(element))")
                appendLine("inline fun ${type.lowercaseName}MutableListOf(vararg elements: ${type.typeName}): ${type.typeName}List = ${type.typeName}ArrayList(elements)")

                appendLine("inline fun ${type.typeName}Array.to${type.typeName}MutableList(offset: Int = 0, length: Int = this.size): ${type.typeName}List = ${type.typeName}ArrayList(this, offset, length)")
            }
        }
    }
}

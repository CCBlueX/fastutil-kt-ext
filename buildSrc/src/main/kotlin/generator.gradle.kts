val generatedDir = layout.buildDirectory.dir("generated/sources")

tasks.named("compileKotlin") {
    dependsOn(generateAllTask)
}

val generateAllTask = tasks.register("generateAll") {
    group = TASK_GROUP
    dependsOn(
        syncUnmodifiableTask,
        pairComponentNTask,
        pairFactoryTask,
        immutableListFactoryTask,
        mutableListFactoryTask,
        mapFastIterableIteratorTask,
    )
}

/**
 * Example:
 * - `IntList.synchronized()`
 * - `IntList.synchronized(lock)`
 * - `IntList.unmodifiable()`
 */
val syncUnmodifiableTask = tasks.register<GenerateSrcTask>("sync-unmodifiable") {
    group = TASK_GROUP

    file.set(generatedDir.map { it.file("sync-unmodifiable.kt") })
    packageName.set("moe.lasoleil.fastutil")
    imports.add("it.unimi.dsi.fastutil.PriorityQueue")
    imports.add("it.unimi.dsi.fastutil.PriorityQueues")
    imports.addAll(IMPORT_ALL)

    content {
        FastutilType.values().forEach { type ->
            for (suffix in arrayOf("BigList", "List", "Set")) {
                val rawType = type.typeName + suffix
                if (type.isGeneric) {
                    appendLine("inline fun <T> ${rawType}<T>.synchronized(): ${rawType}<T> = ${rawType}s.synchronize(this)")
                    appendLine("inline fun <T> ${rawType}<T>.synchronized(lock: Any): ${rawType}<T> = ${rawType}s.synchronize(this, lock)")
                    appendLine("inline fun <T> ${rawType}<T>.unmodifiable(): ${rawType}<T> = ${rawType}s.unmodifiable(this)")
                } else {
                    appendLine("inline fun ${rawType}.synchronized(): $rawType = ${rawType}s.synchronize(this)")
                    appendLine("inline fun ${rawType}.synchronized(lock: Any): $rawType = ${rawType}s.synchronize(this, lock)")
                    appendLine("inline fun ${rawType}.unmodifiable(): $rawType = ${rawType}s.unmodifiable(this)")
                }
            }
        }

        appendLine("inline fun <T> PriorityQueue<T>.synchronized(): PriorityQueue<T> = PriorityQueues.synchronize(this)")
        appendLine("inline fun <T> PriorityQueue<T>.synchronized(lock: Any): PriorityQueue<T> = PriorityQueues.synchronize(this, lock)")
        FastutilType.values().forEach { type ->
            if (type.isGeneric || type == FastutilType.BOOLEAN) {
                return@forEach
            } else {
                val rawType = type.typeName + "PriorityQueue"
                appendLine("inline fun ${rawType}.synchronized(): $rawType = ${rawType}s.synchronize(this)")
                appendLine("inline fun ${rawType}.synchronized(lock: Any): $rawType = ${rawType}s.synchronize(this, lock)")
            }
        }

        forEachMapTypes { left, right ->
            when {
                left.isGeneric && right.isGeneric -> {
                    appendLine("inline fun <K, V> ${left}2${right}Map<K, V>.synchronized() = ${left}2${right}Maps.synchronize(this)")
                    appendLine("inline fun <K, V> ${left}2${right}Map<K, V>.synchronized(lock: Any) = ${left}2${right}Maps.synchronize(this, lock)")
                    appendLine("inline fun <K, V> ${left}2${right}Map<K, V>.unmodifiable() = ${left}2${right}Maps.unmodifiable(this)")
                }
                left.isGeneric -> {
                    appendLine("inline fun <K> ${left}2${right}Map<K>.synchronized() = ${left}2${right}Maps.synchronize(this)")
                    appendLine("inline fun <K> ${left}2${right}Map<K>.synchronized(lock: Any) = ${left}2${right}Maps.synchronize(this, lock)")
                    appendLine("inline fun <K> ${left}2${right}Map<K>.unmodifiable() = ${left}2${right}Maps.unmodifiable(this)")
                }
                right.isGeneric -> {
                    appendLine("inline fun <V> ${left}2${right}Map<V>.synchronized() = ${left}2${right}Maps.synchronize(this)")
                    appendLine("inline fun <V> ${left}2${right}Map<V>.synchronized(lock: Any) = ${left}2${right}Maps.synchronize(this, lock)")
                    appendLine("inline fun <V> ${left}2${right}Map<V>.unmodifiable() = ${left}2${right}Maps.unmodifiable(this)")
                }
                else -> {
                    appendLine("inline fun ${left}2${right}Map.synchronized() = ${left}2${right}Maps.synchronize(this)")
                    appendLine("inline fun ${left}2${right}Map.synchronized(lock: Any) = ${left}2${right}Maps.synchronize(this, lock)")
                    appendLine("inline fun ${left}2${right}Map.unmodifiable() = ${left}2${right}Maps.unmodifiable(this)")
                }
            }
        }
    }
}

/**
 * Example:
 * - `val (left, right) = IntCharPair.of(1, 'c')`
 */
val pairComponentNTask = tasks.register<GenerateSrcTask>("pair-componentN") {
    group = TASK_GROUP

    file.set(generatedDir.map { it.file("pairs-componentN.kt") })
    packageName.set("moe.lasoleil.fastutil")
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
    packageName.set("moe.lasoleil.fastutil")
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

    packageName.set("moe.lasoleil.fastutil")
    imports.addAll(IMPORT_ALL)

    content {
        FastutilType.values().forEach { type ->
            if (type.isGeneric) {
                appendLine("inline fun <T> ${type.lowercaseName}ListOf(): ${type.typeName}List<T> = ${type.typeName}Lists.emptyList()")
                appendLine("inline fun <T> ${type.lowercaseName}ListOf(element: T): ${type.typeName}List<T> = ${type.typeName}Lists.singleton(element)")
                appendLine("inline fun <T> ${type.lowercaseName}ListOf(vararg elements: T): ${type.typeName}List<T> = ${type.typeName}ImmutableList(elements)")

                appendLine("inline fun <T> Array<out T>.as${type.typeName}List(): ${type.typeName}List<T> = ${type.typeName}ImmutableList(this)")
                appendLine("inline fun <T> Array<out T>.as${type.typeName}List(offset: Int = 0, length: Int = this.size): ${type.typeName}List<T> = ${type.typeName}ImmutableList(this, offset, length)")
            } else {
                appendLine("inline fun ${type.lowercaseName}ListOf(): ${type.typeName}List = ${type.typeName}Lists.emptyList()")
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

    packageName.set("moe.lasoleil.fastutil")
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

/**
 * Example:
 * - `Int2ObjectOpenHashMap().fastIterable()`
 * - `Int2ObjectOpenHashMap().fastIterator()`
 */
val mapFastIterableIteratorTask = tasks.register<GenerateSrcTask>("map-fast-iterable-iterator") {
    group = TASK_GROUP

    file.set(generatedDir.map { it.file("map-fast-iterable-iterator.kt") })
    packageName.set("moe.lasoleil.fastutil")
    imports.addAll(IMPORT_ALL)

    content {
        forEachMapTypes { left, right ->
            when {
                left.isGeneric && right.isGeneric -> {
                    appendLine("inline fun <K, V> ${left}2${right}Map<K, V>.fastIterable() = ${left}2${right}Maps.fastIterable(this)")
                    appendLine("inline fun <K, V> ${left}2${right}Map<K, V>.fastIterator() = ${left}2${right}Maps.fastIterator(this)")
                }
                left.isGeneric -> {
                    appendLine("inline fun <K> ${left}2${right}Map<K>.fastIterable() = ${left}2${right}Maps.fastIterable(this)")
                    appendLine("inline fun <K> ${left}2${right}Map<K>.fastIterator() = ${left}2${right}Maps.fastIterator(this)")
                }
                right.isGeneric -> {
                    appendLine("inline fun <V> ${left}2${right}Map<V>.fastIterable() = ${left}2${right}Maps.fastIterable(this)")
                    appendLine("inline fun <V> ${left}2${right}Map<V>.fastIterator() = ${left}2${right}Maps.fastIterator(this)")
                }
                else -> {
                    appendLine("inline fun ${left}2${right}Map.fastIterable() = ${left}2${right}Maps.fastIterable(this)")
                    appendLine("inline fun ${left}2${right}Map.fastIterator() = ${left}2${right}Maps.fastIterator(this)")
                }
            }
        }
    }
}
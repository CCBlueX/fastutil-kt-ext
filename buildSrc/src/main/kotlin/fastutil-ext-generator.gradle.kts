val generatedDir = layout.buildDirectory.dir("generated/sources")

tasks.named("compileKotlin") {
    dependsOn(generateAllTask)
}

val generateAllTask = tasks.register("generate-all") {
    group = TASK_GROUP
    dependsOn(
        syncUnmodifiableTask,
        pairComponentNTask,
//        pairFactoryTask,
        immutableListFactoryTask,
        mutableListFactoryTask,
        mapFastIterableIteratorTask,
        arrayMapToTypedArrayTask,
        collectionMapToTypedArrayTask,
        typedIterableForEachTask,
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

    packageName.set(PACKAGE)
    imports.add("java.util.*")
    imports.add("it.unimi.dsi.fastutil.PriorityQueue")
    imports.add("it.unimi.dsi.fastutil.PriorityQueues")
    imports.addAll(IMPORT_ALL)

    content {
        // For java.util.Collections
        // Java 21+ -> "SequencedCollection", "SequencedMap", "SequencedSet"
        for (rawType in arrayOf("Collection", "List", "Map", "Set", "NavigableMap", "NavigableSet", "SortedMap", "SortedSet")) {
            if (rawType.endsWith("Map")) {
                appendLine("inline fun <K, V> ${rawType}<K, V>.unmodifiable(): ${rawType}<K, V> = Collections.unmodifiable${rawType}(this)")
                appendLine("inline fun <K, V> ${rawType}<K, V>.synchronized(): ${rawType}<K, V> = Collections.synchronized${rawType}(this)")
            } else {
                appendLine("inline fun <T> ${rawType}<T>.unmodifiable(): ${rawType}<T> = Collections.unmodifiable${rawType}(this)")
                appendLine("inline fun <T> ${rawType}<T>.synchronized(): ${rawType}<T> = Collections.synchronized${rawType}(this)")
            }
        }

        forEachTypes { type ->
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
        forEachTypes { type ->
            if (type.isGeneric || type == FastutilType.BOOLEAN) {
                return@forEachTypes
            } else {
                val rawType = type.typeName + "PriorityQueue"
                appendLine("inline fun ${rawType}.synchronized(): $rawType = ${rawType}s.synchronize(this)")
                appendLine("inline fun ${rawType}.synchronized(lock: Any): $rawType = ${rawType}s.synchronize(this, lock)")
            }
        }

        forEachMapTypes { left, right ->
            val rawType = "${left}2${right}Map"
            when {
                left.isGeneric && right.isGeneric -> {
                    appendLine("inline fun <K, V> ${rawType}<K, V>.synchronized(): ${rawType}<K, V> = ${rawType}s.synchronize(this)")
                    appendLine("inline fun <K, V> ${rawType}<K, V>.synchronized(lock: Any): ${rawType}<K, V> = ${rawType}s.synchronize(this, lock)")
                    appendLine("inline fun <K, V> ${rawType}<K, V>.unmodifiable(): ${rawType}<K, V> = ${rawType}s.unmodifiable(this)")
                }
                left.isGeneric -> {
                    appendLine("inline fun <K> ${rawType}<K>.synchronized(): ${rawType}<K> = ${rawType}s.synchronize(this)")
                    appendLine("inline fun <K> ${rawType}<K>.synchronized(lock: Any): ${rawType}<K> = ${rawType}s.synchronize(this, lock)")
                    appendLine("inline fun <K> ${rawType}<K>.unmodifiable(): ${rawType}<K> = ${rawType}s.unmodifiable(this)")
                }
                right.isGeneric -> {
                    appendLine("inline fun <V> ${rawType}<V>.synchronized(): ${rawType}<V> = ${rawType}s.synchronize(this)")
                    appendLine("inline fun <V> ${rawType}<V>.synchronized(lock: Any): ${rawType}<V> = ${rawType}s.synchronize(this, lock)")
                    appendLine("inline fun <V> ${rawType}<V>.unmodifiable(): ${rawType}<V> = ${rawType}s.unmodifiable(this)")
                }
                else -> {
                    appendLine("inline fun ${rawType}.synchronized(): $rawType = ${rawType}s.synchronize(this)")
                    appendLine("inline fun ${rawType}.synchronized(lock: Any): $rawType = ${rawType}s.synchronize(this, lock)")
                    appendLine("inline fun ${rawType}.unmodifiable(): $rawType = ${rawType}s.unmodifiable(this)")
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

    packageName.set(PACKAGE)
    imports.add("it.unimi.dsi.fastutil.Pair")
    imports.addAll(IMPORT_ALL)

    content {
        appendLine("inline operator fun <K, V> Pair<K, V>.component1() = left()")
        appendLine("inline operator fun <K, V> Pair<K, V>.component2() = right()")

        forEachTypes { left ->
            forEachTypes { right ->
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

    packageName.set(PACKAGE)
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

    packageName.set(PACKAGE)
    imports.addAll(IMPORT_ALL)

    content {
        forEachTypes { type ->
            val emptyList = "${type.typeName}Lists.emptyList()"
            fun singletonList(placeholder: String = "element") = "${type.typeName}Lists.singleton($placeholder)"
            if (type.isGeneric) {
                appendLine("inline fun <T> ${type.typeName}List<T>?.orEmpty(): ${type.typeName}List<T> = this ?: $emptyList")
                appendLine("inline fun <T> ${type.lowercaseName}ListOf(): ${type.typeName}List<T> = $emptyList")
                appendLine("inline fun <T> ${type.lowercaseName}ListOf(element: T): ${type.typeName}List<T> = ${singletonList()}")
                appendLine("inline fun <T> ${type.lowercaseName}ListOf(vararg elements: T): ${type.typeName}List<T> =")
                appendLine("when(elements.size) { 0 -> $emptyList 1 -> ${singletonList("elements[0]")} else -> ${type.typeName}ImmutableList(elements) }")

                appendLine("inline fun <T> Array<out T>.as${type.typeName}List(): ${type.typeName}List<T> = ${type.typeName}ImmutableList(this)")
                appendLine("inline fun <T> Array<out T>.as${type.typeName}List(offset: Int = 0, length: Int = this.size): ${type.typeName}List<T> = ${type.typeName}ImmutableList(this, offset, length)")
            } else {
                appendLine("inline fun ${type.typeName}List?.orEmpty(): ${type.typeName}List = this ?: $emptyList")
                appendLine("inline fun ${type.lowercaseName}ListOf(): ${type.typeName}List = $emptyList")
                appendLine("inline fun ${type.lowercaseName}ListOf(element: ${type.typeName}): ${type.typeName}List = ${singletonList()}")
                appendLine("inline fun ${type.lowercaseName}ListOf(vararg elements: ${type.typeName}): ${type.typeName}List =")
                appendLine("when(elements.size) { 0 -> $emptyList 1 -> ${singletonList("elements[0]")} else -> ${type.typeName}ImmutableList(elements) }")

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

    packageName.set(PACKAGE)
    imports.addAll(IMPORT_ALL)

    content {
        forEachTypes { type ->
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
 * - `Int2ObjectMap.fastIterable()`
 * - `Int2ObjectMap.fastIterator()`
 * - `Int2ObjectMap.fastForEach { entry -> ... }`
 * - `Int2ObjectMap.fastForEach { k, v -> ... }`
 */
val mapFastIterableIteratorTask = tasks.register<GenerateSrcTask>("map-fast") {
    group = TASK_GROUP

    packageName.set(PACKAGE)
    imports.add("java.util.function.Consumer")
    imports.addAll(IMPORT_ALL)

    content {
        forEachMapTypes { left, right ->
            val rawType = "${left}2${right}Map"
            val entryRawType = "${rawType}.Entry"
            when {
                left.isGeneric && right.isGeneric -> {
                    appendLine("inline fun <K, V> ${rawType}<K, V>.fastIterable(): ObjectIterable<${entryRawType}<K, V>> = ${rawType}s.fastIterable(this)")
                    appendLine("inline fun <K, V> ${rawType}<K, V>.fastIterator(): ObjectIterator<${entryRawType}<K, V>> = ${rawType}s.fastIterator(this)")
                    appendLine("inline fun <K, V> ${rawType}<K, V>.fastForEach(consumer: Consumer<${entryRawType}<K, V>>) = ${rawType}s.fastForEach(this, consumer)")
                    appendLine("inline fun <K, V> ${rawType}<K, V>.fastForEach(crossinline action: (key: K, value: V) -> Unit) = fastForEach { action(it.key, it.value) }")
                }
                left.isGeneric -> {
                    appendLine("inline fun <K> ${rawType}<K>.fastIterable(): ObjectIterable<${entryRawType}<K>> = ${rawType}s.fastIterable(this)")
                    appendLine("inline fun <K> ${rawType}<K>.fastIterator(): ObjectIterator<${entryRawType}<K>> = ${rawType}s.fastIterator(this)")
                    appendLine("inline fun <K> ${rawType}<K>.fastForEach(consumer: Consumer<${entryRawType}<K>>) = ${rawType}s.fastForEach(this, consumer)")
                    appendLine("inline fun <K> ${rawType}<K>.fastForEach(crossinline action: (key: K, value: $right) -> Unit) = fastForEach { action(it.key, it.${right.lowercaseName}Value) }")
                }
                right.isGeneric -> {
                    appendLine("inline fun <V> ${rawType}<V>.fastIterable(): ObjectIterable<${entryRawType}<V>> = ${rawType}s.fastIterable(this)")
                    appendLine("inline fun <V> ${rawType}<V>.fastIterator(): ObjectIterator<${entryRawType}<V>> = ${rawType}s.fastIterator(this)")
                    appendLine("inline fun <V> ${rawType}<V>.fastForEach(consumer: Consumer<${entryRawType}<V>>) = ${rawType}s.fastForEach(this, consumer)")
                    appendLine("inline fun <V> ${rawType}<V>.fastForEach(crossinline action: (key: $left, value: V) -> Unit) = fastForEach { action(it.${left.lowercaseName}Key, it.value) }")
                }
                else -> {
                    appendLine("inline fun ${rawType}.fastIterable(): ObjectIterable<${entryRawType}> = ${rawType}s.fastIterable(this)")
                    appendLine("inline fun ${rawType}.fastIterator(): ObjectIterator<${entryRawType}> = ${rawType}s.fastIterator(this)")
                    appendLine("inline fun ${rawType}.fastForEach(consumer: Consumer<${entryRawType}>) = ${rawType}s.fastForEach(this, consumer)")
                    appendLine("inline fun ${rawType}.fastForEach(crossinline action: (key: $left, value: $right) -> Unit) = fastForEach { action(it.${left.lowercaseName}Key, it.${right.lowercaseName}Value) }")
                }
            }
        }
    }
}

/**
 * Example:
 * - `arrayOf("xxx").mapToIntArray { it.hashCode() }`
 * - `intArrayOf(...).mapToFloatArray { it * 0.5f }.asFloatList()`
 */
val arrayMapToTypedArrayTask = tasks.register<GenerateSrcTask>("array-map-to-typed-array") {
    group = TASK_GROUP

    packageName.set(PACKAGE)
    imports.addAll(IMPORT_ALL)

    content {
        // Array map to object array
        appendLine("inline fun <T, reified R> Array<out T>.mapToArray(transform: (T) -> R): Array<R> = Array(this.size) { i -> transform(this[i]) }")
        forEachPrimitiveTypes { type ->
            appendLine("inline fun <reified R> ${type}Array.mapToArray(transform: (${type}) -> R): Array<R> = Array(this.size) { i -> transform(this[i]) }")
        }
        // Array map to primitive array
        forEachPrimitiveTypes { result ->
            appendLine("inline fun <T> Array<out T>.mapTo${result}Array(transform: (T) -> ${result}): ${result}Array = ${result}Array(this.size) { i -> transform(this[i]) }")
            forEachPrimitiveTypes { receiver ->
                appendLine("inline fun ${receiver}Array.mapTo${result}Array(transform: (${receiver}) -> ${result}): ${result}Array = ${result}Array(this.size) { i -> transform(this[i]) }")
            }
        }
    }
}

val collectionMapToTypedArrayTask = tasks.register<GenerateSrcTask>("collection-map-to-typed-array") {
    group = TASK_GROUP

    packageName.set(PACKAGE)
    imports.addAll(IMPORT_ALL)

    content {
        // Collection map to object array
        appendLine("inline fun <T, reified R> Collection<T>.mapToArray(transform: (T) -> R): Array<R> = iterator().run { Array(size) { transform(next()) } }")
        forEachTypes { type ->
            if (!type.isGeneric) {
                appendLine("inline fun <reified R> ${type}Collection.mapToArray(transform: (${type}) -> R): Array<R> = iterator().run { Array(size) { transform(next${type}()) } }")
            }
        }
        // Collection map to primitive array
        forEachPrimitiveTypes { result ->
            appendLine("inline fun <T> Collection<T>.mapTo${result}Array(transform: (T) -> ${result}): ${result}Array = iterator().run { ${result}Array(size) { transform(next()) } }")
            forEachPrimitiveTypes { receiver ->
                appendLine("inline fun ${receiver}Collection.mapTo${result}Array(transform: (${receiver}) -> ${result}): ${result}Array = iterator().run { ${result}Array(size) { transform(next${receiver}()) } }")
            }
        }
    }
}

/**
 * Example:
 * - `intListOf().forEachInt { println(it) }`
 */
val typedIterableForEachTask = tasks.register<GenerateSrcTask>("typed-iterable-forEach") {
    group = TASK_GROUP

    packageName.set(PACKAGE)
    imports.addAll(IMPORT_ALL)

    content {
        forEachPrimitiveTypes { type ->
            // Iterator forEach
            appendLine("inline fun ${type}Iterator.forEach${type}(action: (${type}) -> Unit) { while (hasNext()) action(next$type()) }")

            // Iterable forEach
            appendLine("inline fun ${type}Iterable.forEach${type}(action: (${type}) -> Unit) = iterator().forEach(action)")

            // Iterable onEach
            appendLine("inline fun <T : ${type}Iterable> T.onEach${type}(action: (${type}) -> Unit): T = apply { forEach(action) }")

            // Iterable forEachIndexed
            appendLine("inline fun ${type}Iterable.forEach${type}Indexed(action: (index: Int, ${type}) -> Unit) {")
            appendLine("    var index = 0")
            appendLine("    iterator().forEach { action(index++, it) }")
            appendLine("}")
        }
    }
}
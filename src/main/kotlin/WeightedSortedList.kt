package net.ccbluex.fastutil

import it.unimi.dsi.fastutil.Arrays
import it.unimi.dsi.fastutil.doubles.DoubleArrayList
import it.unimi.dsi.fastutil.objects.*
import java.util.*
import java.util.function.Predicate
import java.util.function.ToDoubleFunction
import java.util.function.UnaryOperator
/**
 * A list of elements kept sorted by their associated weight (non-decreasing).
 *
 * This collection stores items and their corresponding weights in parallel arrays.
 * The weights must always satisfy:
 *  - all weights are within the configured `[lowerBound, upperBound]` (bounds may be exclusive),
 *  - the weights array is non-decreasing (monotonic non-decreasing),
 *  - `items.size == weights.size`.
 *
 * The list exposes the usual `List`-like operations, but enforces weight constraints
 * and preserves sorted order on all mutating operations. Elements are compared by
 * the double weight returned by the supplied `weighter` function (not by element
 * natural order or comparator).
 *
 * This class implements `Object2DoubleFunction<E>` so you can query an element's
 * weight via `getDouble(key)`. A `defaultReturnValue` is used when a key is not present.
 *
 * Thread-safety: not thread-safe — external synchronization required for concurrent access.
 *
 * Typical time complexities (n = size):
 *  - `add(e)`: O(log n) to locate position + O(n) to insert (worst-case shifting) → O(n)
 *  - `removeAt(index)`: O(n) to shift elements → O(n)
 *  - `get` / index-based reads: O(1)
 *  - `getDouble(key)`: O(n) (uses indexOf)
 *
 * Example:
 * ```
 * // constructor takes lower/upper bounds and a weighter function:
 * val list = WeightedSortedList<String>(lowerBound = 0.0, upperBound = 1.0, weighter = { s -> s.length.toDouble() })
 * list.add("a") // inserted according to weight
 * ```
 */
class WeightedSortedList<E>
/**
 * @param items internal list of elements (parallel to `weights`).
 *              Must have same initial size as `weights`.
 * @param weights internal list of weights (parallel to `items`).
 *                Must be non-decreasing and each value must be within the configured bounds.
 * @param lowerBound lower bound (inclusive or exclusive depending on `lowerBoundInclusive`) for allowed weights.
 * @param lowerBoundInclusive whether `lowerBound` itself is allowed.
 * @param upperBound upper bound (inclusive or exclusive depending on `upperBoundInclusive`) for allowed weights.
 * @param upperBoundInclusive whether `upperBound` itself is allowed.
 * @param weighter function mapping an element to its weight (double). This function is consulted
 *                 on all insert/replace operations to determine the element's weight.
 *
 * Note: constructor performs validation of `items` and `weights` invariants and will throw
 * IllegalArgumentException if sizes mismatch, if any weight is out of bounds, or if weights are not non-decreasing.
 */
private constructor(
    private val items: ObjectArrayList<E>,
    private val weights: DoubleArrayList,
    val lowerBound: Double,
    val lowerBoundInclusive: Boolean,
    val upperBound: Double,
    val upperBoundInclusive: Boolean,
    val weighter: ToDoubleFunction<in E>,
) : ObjectList<E> by items, RandomAccess, Object2DoubleFunction<E> {

    init {
        if (items.size != weights.size) {
            throw IllegalArgumentException("items and weights must have same size")
        }
        for (i in 0 until weights.size) {
            val w = weights.getDouble(i)
            if (!inBounds(w)) throw IllegalArgumentException("weight at index $i out of bounds: $w")
            if (i > 0) {
                val prev = weights.getDouble(i - 1)
                if (prev > w) throw IllegalArgumentException("weights must be non-decreasing: index ${i-1} has $prev, index $i has $w")
            }
        }
    }
    /**
     * Convenience constructor that creates empty internal storage with the given default capacity.
     *
     * @param defaultCapacity initial capacity used to allocate internal arrays (may be 0).
     * @param lowerBound lower bound for allowed weights.
     * @param lowerBoundInclusive whether the lower bound is inclusive.
     * @param upperBound upper bound for allowed weights.
     * @param upperBoundInclusive whether the upper bound is inclusive.
     * @param weighter function mapping an element to its weight.
     *
     * The same invariants on bounds and ordering apply as for the primary constructor.
     */
    constructor(
        defaultCapacity: Int = 0,
        lowerBound: Double = Double.MIN_VALUE,
        lowerBoundInclusive: Boolean = true,
        upperBound: Double = Double.MAX_VALUE,
        upperBoundInclusive: Boolean = true,
        weighter: ToDoubleFunction<in E>,
    ) : this(
        ObjectArrayList<E>(defaultCapacity),
        DoubleArrayList(defaultCapacity),
        lowerBound,
        lowerBoundInclusive,
        upperBound,
        upperBoundInclusive,
        weighter,
    )

    /**
     * Default double value returned by `getDouble(key)` when the element is not found.
     *
     * This value can be read/written via `defaultReturnValue()` and `defaultReturnValue(rv)`.
     */
    private var defaultReturnValue = 0.0

    /**
     * @see AbstractObjectList.ensureIndex
     */
    private fun ensureIndex(index: Int) {
        if (index < 0) throw IndexOutOfBoundsException("Index ($index) is negative")
        if (index > size) throw IndexOutOfBoundsException("Index ($index) is greater than list size ($size)")
    }

    /**
     * @see AbstractObjectList.ensureRestrictedIndex
     */
    private fun ensureRestrictedIndex(index: Int) {
        if (index < 0) throw IndexOutOfBoundsException("Index ($index) is negative")
        if (index >= size) throw IndexOutOfBoundsException("Index ($index) is greater than or equal to list size ($size)")
    }
    
    private fun inLowerBound(w: Double): Boolean {
        return if (lowerBoundInclusive) w >= lowerBound else w > lowerBound
    }

    private fun inUpperBound(w: Double): Boolean {
        return if (upperBoundInclusive) w <= upperBound else w < upperBound
    }

    private fun inBounds(w: Double): Boolean = inLowerBound(w) && inUpperBound(w)

    /**
     * Locate the insertion index for a given weight `w` using binary search.
     *
     * The returned index is the first position `i` such that `weights[i] > w`.
     * This yields a stable insertion point that preserves non-decreasing order
     * (elements with equal weight are inserted after existing equal-weight elements).
     *
     * Complexity: O(log n).
     *
     * @param w weight to locate insertion position for
     * @return insertion index in range [0, size]
     */
    private fun findInsertIndexForWeight(w: Double): Int {
        var lo = 0
        var hi = weights.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            val wm = weights.getDouble(mid)
            if (wm <= w) {
                lo = mid + 1
            } else {
                hi = mid
            }
        }
        return lo
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun computeWeight(e: E): Double = weighter.applyAsDouble(e)

    /**
     * Insert `element` into the list at the correct sorted position according to its weight.
     *
     * - Computes the element's weight via `weighter`.
     * - If the weight is out of the configured bounds, the element is **not** inserted and the method returns false.
     * - Otherwise, finds the insertion index (binary search) and inserts the element and its weight.
     *
     * @param element element to insert
     * @return `true` if the element was inserted, `false` if its weight is out of bounds
     *
     * Complexity: O(n) due to potential shifting of elements; O(log n) to find insertion index.
     */
    override fun add(element: E): Boolean {
        val w = computeWeight(element)
        if (!inBounds(w)) return false
        val idx = findInsertIndexForWeight(w)
        items.add(idx, element)
        weights.add(idx, w)
        return true
    }

    /**
     * Insert `element` at the specified index while preserving the sorted-by-weight invariant.
     *
     * Preconditions:
     *  - `index` must be a valid insertion index (0 <= index <= size).
     *  - The computed weight must be within bounds.
     *  - The weight must be >= weight at `index - 1` (if index > 0) and <= weight at `index` (if index < size)
     *    to preserve non-decreasing order.
     *
     * @param index insertion index
     * @param element element to insert
     * @throws IndexOutOfBoundsException if `index` is invalid
     * @throws IllegalStateException if the element's weight is out of bounds or inserting at `index` would break sorted order
     */
    override fun add(index: Int, element: E) {
        ensureIndex(index)
        val w = computeWeight(element)
        if (!inBounds(w)) {
            throw IllegalStateException("Element weight $w out of allowed bounds [$lowerBound${if (lowerBoundInclusive) " (inc)" else ""}, $upperBound${if (upperBoundInclusive) " (inc)" else ""}]")
        }
        // must fit relative to neighbors to keep sorted order
        val leftOk = if (index == 0) true else weights.getDouble(index - 1) <= w
        val rightOk = if (index == size) true else w <= weights.getDouble(index)
        if (!leftOk || !rightOk) {
            throw IllegalStateException("Inserting element with weight $w at index $index would break sorted order (leftOk=$leftOk, rightOk=$rightOk)")
        }
        items.add(index, element)
        weights.add(index, w)
    }

    /**
     * Add all elements from the given collection while preserving the sorted-by-weight invariant.
     *
     * Behavior:
     *  - Each element’s weight is computed via the configured `weighter`.
     *  - Elements with out-of-bounds weights are skipped and not added.
     *  - Valid elements are collected, internally sorted by their weight in non-decreasing order,
     *    and then merged into the current list so that overall order is preserved.
     *
     * The method returns `true` if at least one element was successfully inserted.
     *
     * Notes:
     *  - The final list remains globally sorted by weight after the operation.
     *  - Insertions are stable for elements with equal weights (existing items remain before newly added ones).
     *  - Complexity: O(m log m + n) for m new elements and n existing items in the list (due to sorting and merging).
     *
     * @param elements collection of elements to add
     * @return `true` if the list changed as a result, `false` otherwise
     */
    override fun addAll(elements: Collection<E>): Boolean {
        if (elements.isEmpty()) return false

        val oldN = size
        val incomingN = elements.size
        // collect valid elements (in-bounds)
        val resEls = arrayOfNulls<Any?>(incomingN)
        val resWs = DoubleArray(incomingN)
        var validIncomingN = 0
        for (e in elements) {
            val w = computeWeight(e)
            if (inBounds(w)) {
                // skip out-of-bounds
                resEls[validIncomingN] = e
                resWs[validIncomingN] = w
                validIncomingN++
            }
        }
        if (validIncomingN == 0) return false

        val k = validIncomingN

        // stable sort by weight
        if (k > 1) {
            Arrays.quickSort(0, k, { i, j -> resWs[i].compareTo(resWs[j]) }) { i, j ->
                resEls.swap(i, j)
                resWs.swap(i, j)
            }
        }

        val newSize = oldN + k
        items.size(newSize)
        weights.size(newSize)
        val innerEls = items.elements()
        val innerWs = weights.elements()
        @Suppress("UNCHECKED_CAST")
        resEls.copyInto(innerEls as Array<Any?>, oldN, 0, k)
        resWs.copyInto(innerWs, oldN, 0, k)
        if (newSize > 1 && oldN > 0 && oldN < newSize) {
            // Merge sort (0..<oldN + oldN..<size)
            for (i in oldN until newSize) {
                val curW = innerWs[i]
                val curEl = innerEls[i]
                var j = i - 1

                while (j >= 0 && innerWs[j] > curW) {
                    innerWs[j + 1] = innerWs[j]
                    innerEls[j + 1] = innerEls[j]
                    j--
                }

                innerWs[j + 1] = curW
                innerEls[j + 1] = curEl
            }
        }

        return true
    }

    /**
     * Insert a contiguous block of elements at `index`.
     *
     * Preconditions checked before mutation:
     *  - `index` must be a valid insertion index.
     *  - None of the elements' weights may be out of the configured bounds.
     *  - The computed weights for the block must be non-decreasing internally (block must be sorted).
     *  - The first weight in the block must be >= weight at `index - 1` (if index > 0).
     *  - The last weight in the block must be <= weight at `index` (if index < size).
     *
     * If any condition fails the method returns `false` and the list is unchanged.
     *
     * @param index insertion index
     * @param elements collection of elements for the block
     * @return true if elements were inserted, false otherwise
     * @throws IndexOutOfBoundsException if index invalid
     */
    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        ensureIndex(index)
        if (elements.isEmpty()) return false
        // compute weights and check each in bounds
        val elementCount = elements.size
        val ws = DoubleArray(elementCount)
        val es = arrayOfNulls<Any?>(elementCount)
        elements.forEachIndexed { i, e ->
            val w = computeWeight(e)
            if (!inBounds(w)) return false
            ws[i] = w
            es[i] = e
        }
        // the block's weights must be non-decreasing (we will insert them as a contiguous block)
        for (i in 1 until ws.size) {
            if (ws[i - 1] > ws[i]) return false
        }
        // must fit into neighbors at index
        if (index > 0) {
            val left = weights.getDouble(index - 1)
            if (ws[0] < left) return false
        }
        if (index < size) {
            val right = weights.getDouble(index)
            if (ws[ws.size - 1] > right) return false
        }
        // all good: insert
        items.addElements(index, es)
        weights.addElements(index, ws)
        return true
    }

    /**
     * Remove all elements and weights from the list.
     *
     * After this call, `size == 0`.
     */
    override fun clear() {
        items.clear()
        weights.clear()
    }

    /**
     * Delegates to `listIterator()`.
     */
    override fun iterator(): ObjectListIterator<E> = listIterator()

    /**
     * Delegates to `listIterator(0)`.
     */
    override fun listIterator(): ObjectListIterator<E> = listIterator(0)

    /**
     * Return a fail-safe iterator over the list elements (a custom `ObjectListIterator`).
     *
     * The iterator supports `add`, `remove`, `set` operations but will enforce the same
     * weight bounds & ordering invariants as direct list operations.
     *
     * The returned iterator's behavior:
     *  - `remove()` and `set(e)` are allowed only when the iterator's last returned index (`lastRet`) is valid;
     *    otherwise they throw `IllegalStateException`.
     *  - `add(e)` inserts at the iterator's current cursor position; the inserted element's weight
     *    must fit between the left and right neighbors or an `IllegalStateException` is thrown.
     *
     * @param index starting index for the iterator (use `listIterator()` for index 0)
     * @throws IndexOutOfBoundsException if `index` is invalid
     */
    override fun listIterator(index: Int): ObjectListIterator<E> {
        ensureIndex(index)
        return WeightedListIterator(index)
    }

    /**
     * Remove the first occurrence of `element` from the list (and its weight).
     *
     * @param element element to remove
     * @return true if the element was present and removed, false otherwise
     *
     * Complexity: O(n) to locate and shift elements.
     */
    override fun remove(element: E): Boolean {
        val idx = items.indexOf(element)
        if (idx == -1) return false
        items.removeAt(idx)
        weights.removeDouble(idx)
        return true
    }

    /**
     * Remove and return the element at `index`, also removing the corresponding weight.
     *
     * @param index index of element to remove
     * @return removed element
     * @throws IndexOutOfBoundsException if `index` invalid
     *
     * Complexity: O(n) due to shift.
     */
    override fun removeAt(index: Int): E {
        ensureRestrictedIndex(index)
        return items.removeAt(index).also { weights.removeDouble(index) }
    }

    /**
     * Removes all elements that are contained in the given collection.
     *
     * All bulk operations preserve the weight array alignment with items and are performed in a single
     * pass to avoid repeated shifting.
     *
     * @return true if the list changed as a result.
     */
    override fun removeAll(elements: Collection<E>): Boolean = bulkRemove { it in elements }

    /**
     * Retains only elements that are contained in the given collection.
     *
     * All bulk operations preserve the weight array alignment with items and are performed in a single
     * pass to avoid repeated shifting.
     *
     * @return true if the list changed as a result.
     */
    override fun retainAll(elements: Collection<E>): Boolean = bulkRemove { it !in elements }

    /**
     * Removes elements matching the given predicate.
     *
     * All bulk operations preserve the weight array alignment with items and are performed in a single
     * pass to avoid repeated shifting.
     *
     * @return true if the list changed as a result.
     */
    override fun removeIf(filter: Predicate<in E>): Boolean = bulkRemove { filter.test(it) }

    /**
     * Internal single-pass compaction routine.
     *
     * Iterates items and weights arrays, keeping items for which `predicate(item)` is false.
     * The arrays are overwritten in-place and the list size trimmed to the new length.
     *
     * Returns true if any element was removed.
     *
     * Complexity: O(n).
     */
    private inline fun bulkRemove(predicate: (E) -> Boolean): Boolean {
        val itemsArr = items.elements()
        val weightsArr = weights.elements()
        var j = 0
        val s = size
        for (i in 0 until s) {
            if (!predicate(itemsArr[i])) {
                itemsArr[j] = itemsArr[i]
                weightsArr[j] = weightsArr[i]
                j++
            }
        }
        size(j)
        return j < s
    }

    /**
     * Replace the element at `index` with `element`, updating its weight as computed by `weighter`.
     *
     * Preconditions:
     *  - `index` must be valid.
     *  - new weight must be within bounds.
     *  - new weight must be >= left neighbor weight (if present) and <= right neighbor weight (if present).
     *
     * If these checks fail, an `IllegalStateException` is thrown and the list remains unchanged.
     *
     * @param index index to replace
     * @param element new element
     * @return the previous element at `index`
     * @throws IndexOutOfBoundsException if index invalid
     * @throws IllegalStateException if new weight is out of bounds or violates neighboring order
     *
     * Complexity: O(1) for update.
     */
    override fun set(index: Int, element: E): E {
        ensureRestrictedIndex(index)
        val w = computeWeight(element)
        if (!inBounds(w)) throw IllegalStateException("Element weight $w out of bounds")
        // check neighbors
        if (index > 0) {
            val left = weights.getDouble(index - 1)
            if (w < left) throw IllegalStateException("Setting element at index $index with weight $w would be < left neighbor weight $left")
        }
        if (index + 1 < size) {
            val right = weights.getDouble(index + 1)
            if (w > right) throw IllegalStateException("Setting element at index $index with weight $w would be > right neighbor weight $right")
        }
        val old = items.set(index, element)
        weights.set(index, w)
        return old
    }

    /**
     * Resize the list to the given size (shrinking only).
     *
     * Behavior:
     *  - If `size` < 0 => IllegalArgumentException.
     *  - If `size` > current size => UnsupportedOperationException (growing not supported).
     *  - If size <= current size => trims both items and weights to the given size.
     *
     * This method manipulates internal arrays directly and is used by internal routines.
     */
    override fun size(size: Int) {
        if (size < 0) throw IllegalArgumentException("size < 0")
        if (size > this.size) throw UnsupportedOperationException("Growing size not supported")
        items.size(size)
        weights.size(size)
    }

    /**
     * Remove a range of elements [from, to) and corresponding weights.
     *
     * If `from == to` nothing happens.
     *
     * @throws IndexOutOfBoundsException if indices invalid (delegated to underlying lists).
     */
    override fun removeElements(from: Int, to: Int) {
        if (from == to) return
        items.removeElements(from, to)
        weights.removeElements(from, to)
    }

    /**
     * Insert a contiguous block of elements supplied as a raw array slice.
     *
     * Preconditions checked:
     *  - `offset` and `length` must define a valid slice of `a`.
     *  - no element in slice may be null (throws NullPointerException).
     *  - each element's computed weight must be within bounds.
     *  - the block's weights must be non-decreasing internally.
     *  - the block must fit between neighbor weights at `index`.
     *
     * On success, elements and weights are inserted as a contiguous block.
     *
     * @param index insertion index
     * @param a source array
     * @param offset offset in `a`
     * @param length number of elements to insert
     * @throws IndexOutOfBoundsException for invalid index/offset/length
     * @throws NullPointerException for null element in the array slice
     * @throws IllegalStateException if any weight is out of bounds or order/fit checks fail
     */
    override fun addElements(index: Int, a: Array<out E?>, offset: Int, length: Int) {
        if (length == 0) return
        if (offset < 0 || offset + length > a.size) throw IndexOutOfBoundsException("offset/length")
        ensureIndex(index)
        // prepare weights and elements, check in bounds and non-decreasing using raw arrays
        val ws = DoubleArray(length)
        val es = arrayOfNulls<Any?>(length)
        for (i in 0 until length) {
            val e = a[offset + i] ?: throw NullPointerException("null element")
            val w = computeWeight(e)
            if (!inBounds(w)) throw IllegalStateException("Element weight $w out of bounds")
            ws[i] = w
            @Suppress("UNCHECKED_CAST")
            es[i] = e as E
        }
        for (i in 1 until length) {
            if (ws[i - 1] > ws[i]) throw IllegalStateException("Added block must be non-decreasing by weight")
        }
        // check fit at index
        if (index > 0) {
            val left = weights.getDouble(index - 1)
            if (ws[0] < left) throw IllegalStateException("Block start weight ${ws[0]} < left neighbor $left")
        }
        if (index < size) {
            val right = weights.getDouble(index)
            if (ws[length - 1] > right) throw IllegalStateException("Block end weight ${ws[length - 1]} > right neighbor $right")
        }
        // insert
        items.addElements(index, es)
        weights.addElements(index, ws)
    }

    /**
     * Return the weight associated with `key` (first occurrence), or the configured `defaultReturnValue`
     * if the key is not present.
     *
     * This method performs a linear search (`indexOf`) to locate the key, so complexity is O(n).
     *
     * @param key the element to query
     * @return weight for `key`, or `defaultReturnValue` if absent
     */
    override fun getDouble(key: Any?): Double {
        val idx = items.indexOf(key)
        return if (idx == -1) defaultReturnValue else weights.getDouble(idx)
    }

    /**
     * Replace every element by applying the given operator and update weights accordingly.
     *
     * Implementation notes:
     *  - Computes new element array and weights first.
     *  - Validates that all new weights are within bounds and that the resulting weights
     *    array is non-decreasing. If validation fails, an IllegalStateException is thrown
     *    and no mutation occurs.
     *  - If validation passes, internal arrays are overwritten in bulk.
     *
     * @param operator mapping function from old element to new element
     * @throws IllegalStateException if any replacement yields an out-of-bounds weight or the
     *                               resulting weights are not non-decreasing
     */
    override fun replaceAll(operator: UnaryOperator<E>) {
        val n = items.size
        if (n == 0) return
        // compute new arrays first
        val newEls = arrayOfNulls<Any?>(n)
        val newWs = DoubleArray(n)
        for (i in 0 until n) {
            val e = operator.apply(items[i])
            val w = computeWeight(e)
            if (!inBounds(w)) throw IllegalStateException("Replacement at index $i has weight $w out of bounds")
            newEls[i] = e
            newWs[i] = w
        }
        // ensure non-decreasing
        for (i in 1 until n) {
            if (newWs[i - 1] > newWs[i]) {
                throw IllegalStateException("Replacement results in non-sorted weights at position ${i - 1} and $i: ${newWs[i - 1]} > ${newWs[i]}")
            }
        }
        // apply
        @Suppress("UNCHECKED_CAST")
        newEls.copyInto(items.elements() as Array<Any?>)
        newWs.copyInto(weights.elements())
    }

    /**
     * Not supported. Sorting by element comparator is not allowed because weights determine element order.
     *
     * @throws UnsupportedOperationException always
     */
    override fun sort(c: Comparator<in E>?) {
        throw UnsupportedOperationException("Not supported")
    }

    /**
     * Return an unmodifiable subList view of the underlying items between [from, to).
     *
     * Note: the returned list is unmodifiable and will not reflect weight operations directly.
     *
     * @return an unmodifiable view of the specified range of items
     */
    override fun subList(from: Int, to: Int): ObjectList<E> {
        return items.subList(from, to).unmodifiable()
    }

    /**
     * Convenience wrapper that returns whether the given element is contained in the list.
     *
     * Equivalent to `contains(key)`.
     */
    override fun containsKey(key: Any?): Boolean {
        return contains(key)
    }

    /**
     * Getter for the default return value used by `getDouble` when a key is not found.
     *
     * See also `defaultReturnValue(rv)` to set this value.
     */
    override fun defaultReturnValue(): Double {
        return defaultReturnValue
    }

    /**
     * Setter for the default return value used by `getDouble` when a key is not found.
     *
     * @param rv new default return value
     */
    override fun defaultReturnValue(rv: Double) {
        defaultReturnValue = rv
    }

    /**
     * Equality considers:
     *  - same lower/upper bounds and inclusivity flags,
     *  - same defaultReturnValue,
     *  - equality of the `items` list and `weights` list.
     *
     * Two WeightedSortedList instances with identical contents and the same bounds are equal.
     *
     * Don't rely on the result value, it might be changed in the future.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WeightedSortedList<*>) return false

        if (lowerBound != other.lowerBound) return false
        if (lowerBoundInclusive != other.lowerBoundInclusive) return false
        if (upperBound != other.upperBound) return false
        if (upperBoundInclusive != other.upperBoundInclusive) return false
        if (defaultReturnValue != other.defaultReturnValue) return false
        if (items != other.items) return false
        if (weights != other.weights) return false

        return true
    }

    /**
     * Compute hash code consistent with equals: combines items, weights, bounds flags and default return value.
     *
     * Don't rely on the result value, it might be changed in the future.
     */
    override fun hashCode(): Int {
        var result = items.hashCode()
        result = 31 * result + weights.hashCode()
        result = 31 * result + lowerBound.hashCode()
        result = 31 * result + lowerBoundInclusive.hashCode()
        result = 31 * result + upperBound.hashCode()
        result = 31 * result + upperBoundInclusive.hashCode()
        result = 31 * result + defaultReturnValue.hashCode()
        return result
    }

    /**
     * Return a string representation containing each element and its weight in order as `element(weight)`.
     *
     * Special handling: if an element is the same instance as the enclosing list, it prints `(this list)`.
     *
     * Don't rely on the result value, it might be changed in the future.
     */
    override fun toString(): String = buildString {
        append("[")
        for (i in 0 until size) {
            if (i != 0) append(", ")
            val item = items[i]
            val weight = weights.getDouble(i)
            append(if (item === this@WeightedSortedList) "(this list)" else item.toString())
            append("(")
            append(weight)
            append(")")
        }
        append("]")
    }

    /**
     * A bidirectional list iterator that enforces bound and ordering invariants on mutating iterator operations.
     *
     * Internal state:
     *  - `cursor` points to the index of the element that would be returned by `next()`.
     *  - `lastRet` is the index of the last element returned by `next()` or `previous()` (or -1 if none).
     *
     * Operation rules:
     *  - `next()` / `previous()` update `lastRet` and `cursor`.
     *  - `remove()` and `set(e)` are valid only when `lastRet != -1`; otherwise they throw `IllegalStateException`.
     *  - `add(e)` attempts to insert at `cursor`; the weight of `e` must fit between neighbors or `IllegalStateException` is thrown.
     *
     * Exceptions: methods throw `NoSuchElementException`, `IllegalStateException`, or `IllegalStateException`
     * when bounds/order checks fail, consistent with `ListIterator` semantics.
     */
    private inner class WeightedListIterator(startIndex: Int) : ObjectListIterator<E> {
        private var cursor = startIndex // next index
        private var lastRet = -1

        /**
         * Return whether there is a next (or previous) element relative to the cursor.
         */
        override fun hasNext(): Boolean = cursor < size

        override fun hasPrevious(): Boolean = cursor > 0

        override fun nextIndex(): Int = cursor

        override fun previousIndex(): Int = cursor - 1

        /**
         * Insert `e` at the iterator's current cursor position.
         *
         * Preconditions:
         *  - weight must be within bounds
         *  - weight must be >= left neighbor weight (if present) and <= right neighbor weight (if present)
         *
         * On success the cursor is advanced past the inserted element and `lastRet` is reset to -1
         * (consistent with `ListIterator.add` semantics).
         *
         * @throws IllegalStateException if weight out of bounds or insertion would break ordering.
         */
        override fun add(e: E) {
            val w = computeWeight(e)
            if (!inBounds(w)) throw IllegalStateException("Element weight $w out of bounds")
            // must fit between previous element (cursor-1) and next element (cursor)
            val leftOk = if (cursor == 0) true else weights.getDouble(cursor - 1) <= w
            val rightOk = if (cursor == size) true else w <= weights.getDouble(cursor)
            if (!leftOk || !rightOk) {
                throw IllegalStateException("Iterator.add would break order for weight $w at cursor $cursor")
            }
            items.add(cursor, e)
            weights.add(cursor, w)
            cursor++
            lastRet = -1
        }

        /**
         * Remove the element most recently returned by `next()` or `previous()`.
         *
         * @throws IllegalStateException if there is no such element (i.e. `lastRet == -1`).
         */
        override fun remove() {
            if (lastRet == -1) throw IllegalStateException("Illegal state: nothing to remove")
            items.removeAt(lastRet)
            weights.removeDouble(lastRet)
            if (lastRet < cursor) cursor--
            lastRet = -1
        }

        /**
         * Replace the element most recently returned by `next()`/`previous()` with `e`.
         *
         * Preconditions:
         *  - `lastRet` must be valid (otherwise IllegalStateException).
         *  - `e`'s weight must be within bounds and not violate neighbors.
         *
         * @throws IllegalStateException on invalid state or weight/order violations.
         */
        override fun set(e: E) {
            if (lastRet == -1) throw IllegalStateException("Illegal state: nothing to set")
            val w = computeWeight(e)
            if (!inBounds(w)) throw IllegalStateException("Element weight $w out of bounds")
            // check neighbors around lastRet
            if (lastRet > 0) {
                val left = weights.getDouble(lastRet - 1)
                if (w < left) throw IllegalStateException("Iterator.set would break order: $w < left $left")
            }
            if (lastRet + 1 < size) {
                val right = weights.getDouble(lastRet + 1)
                if (w > right) throw IllegalStateException("Iterator.set would break order: $w > right $right")
            }
            items[lastRet] = e
            weights.set(lastRet, w)
        }

        override fun next(): E {
            if (!hasNext()) throw NoSuchElementException()
            val e = items[cursor]
            lastRet = cursor
            cursor++
            return e
        }

        override fun previous(): E {
            if (!hasPrevious()) throw NoSuchElementException()
            cursor--
            val e = items[cursor]
            lastRet = cursor
            return e
        }
    }

}

/**
 * Original `addElements` requires a typed array
 *
 * @see ObjectArrayList.grow
 * @see ObjectArrayList.size
 * @see ObjectArrayList.addElements
 */
private fun ObjectArrayList<*>.addElements(index: Int, a: Array<out Any?>, offset: Int, length: Int) {
    // assert: index, offset, length are legal value
    size(size + length)
    System.arraycopy(this.elements(), index, this.elements(), index + length, size - length - index)
    System.arraycopy(a, offset, this.elements(), index, length)
}

/**
 * Original `addElements` requires a typed array
 *
 * @see ObjectArrayList.grow
 * @see ObjectArrayList.size
 * @see ObjectArrayList.addElements
 */
private fun ObjectArrayList<*>.addElements(index: Int, a: Array<out Any?>) {
    addElements(index, a, 0, a.size)
}

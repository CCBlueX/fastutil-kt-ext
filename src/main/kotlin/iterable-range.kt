package net.ccbluex.fastutil

import it.unimi.dsi.fastutil.doubles.AbstractDoubleList
import it.unimi.dsi.fastutil.doubles.DoubleList
import it.unimi.dsi.fastutil.doubles.DoubleLists
import it.unimi.dsi.fastutil.floats.AbstractFloatList
import it.unimi.dsi.fastutil.floats.FloatList
import it.unimi.dsi.fastutil.floats.FloatLists
import kotlin.math.floor

private const val EPSILON_D = 1e-10
private const val EPSILON_F = 1e-6f

// https://stackoverflow.com/questions/44315977/ranges-in-kotlin-using-data-type-double

/**
 * Returns a read-only [DoubleList] representing this [ClosedRange] of [Double] values
 * stepped by the specified [step].
 *
 * The returned list is **virtual**: elements are not stored in memory, but computed
 * on demand when accessed by index. This provides **O(1) memory usage** and supports
 * random access.
 *
 * The size of the list is calculated based on the range and the step, with a small
 * epsilon ([EPSILON_D]) to account for floating-point inaccuracies.
 *
 * Example:
 * ```
 * val list = (0.0..1.0).step(0.25)
 * println(list) // [0.0, 0.25, 0.5, 0.75, 1.0]
 * println(list[2]) // 0.5
 * ```
 *
 * Notes:
 * - [step] must be positive.
 * - The range must contain finite values.
 * - The returned list is **read-only**; calls to `add` or `remove` will throw
 *   [UnsupportedOperationException].
 *
 * @param step the step size between consecutive elements, must be > 0.
 * @return a read-only [DoubleList] representing the stepped range.
 * @throws IllegalArgumentException if `step` is not positive or the range contains
 *   non-finite values.
 */
infix fun ClosedRange<Double>.step(step: Double): DoubleList {
    require(start.isFinite())
    require(endInclusive.isFinite())
    require(step > 0.0)

    if (isEmpty()) return DoubleLists.emptyList()

    val forward = start <= endInclusive
    val distance = if (forward) endInclusive - start else start - endInclusive

    val rawSteps = distance / step
    val steps = floor(rawSteps).toInt()
    val size = if (step * steps <= distance + EPSILON_D) steps + 1 else steps

    return object : AbstractDoubleList(), RandomAccess {
        override fun getDouble(index: Int): Double {
            super.ensureRestrictedIndex(index)
            return if (forward) start + step * index else start - step * index
        }

        override fun add(k: Double) = throw UnsupportedOperationException("Readonly list")

        override val size: Int get() = size
    }
}

/**
 * Returns a read-only [FloatList] representing this [ClosedRange] of [Float] values
 * stepped by the specified [step].
 *
 * The returned list is **virtual**: elements are not stored in memory, but computed
 * on demand when accessed by index. This provides **O(1) memory usage** and supports
 * random access.
 *
 * The size of the list is calculated based on the range and the step, with a small
 * epsilon ([EPSILON_F]) to account for floating-point inaccuracies.
 *
 * Example:
 * ```
 * val list = (0f..1f).step(0.25f)
 * println(list) // [0.0, 0.25, 0.5, 0.75, 1.0]
 * println(list[2]) // 0.5
 * ```
 *
 * Notes:
 * - [step] must be positive.
 * - The range must contain finite values.
 * - The returned list is **read-only**; calls to `add` or `remove` will throw
 *   [UnsupportedOperationException].
 *
 * @param step the step size between consecutive elements, must be > 0.
 * @return a read-only [FloatList] representing the stepped range.
 * @throws IllegalArgumentException if `step` is not positive or the range contains
 *   non-finite values.
 */
infix fun ClosedRange<Float>.step(step: Float): FloatList {
    require(start.isFinite())
    require(endInclusive.isFinite())
    require(step > 0.0f)

    if (isEmpty()) return FloatLists.emptyList()

    val forward = start <= endInclusive
    val distance = if (forward) endInclusive - start else start - endInclusive

    val rawSteps = distance / step
    val steps = floor(rawSteps).toInt()
    val size = if (step * steps <= distance + EPSILON_F) steps + 1 else steps

    return object : AbstractFloatList(), RandomAccess {
        override fun getFloat(index: Int): Float {
            super.ensureRestrictedIndex(index)
            return if (forward) start + step * index else start - step * index
        }

        override fun add(k: Float) = throw UnsupportedOperationException("Readonly list")

        override val size: Int get() = size
    }
}

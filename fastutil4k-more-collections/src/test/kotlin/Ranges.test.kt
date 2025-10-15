package net.ccbluex.fastutil

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RangesTest {

    @Test
    fun testDoubleStepBasic() {
        val list = (0.0..1.0).step(0.25)
        assertEquals(doubleListOf(0.0, 0.25, 0.5, 0.75, 1.0), list)

        assertEquals(0.5, list.getDouble(2))
        assertEquals(list.size, 5)
    }

    @Test
    fun testFloatStepBasic() {
        val list = (0f..1f).step(0.25f)
        assertEquals(floatListOf(0f, 0.25f, 0.5f, 0.75f, 1f), list)
        assertEquals(0.5f, list.getFloat(2))
        assertEquals(list.size, 5)
    }

    @Test
    fun testDoubleStepWithEpsilon() {
        val list = (0.0..1.0).step(0.3)
        val expected = doubleListOf(0.0, 0.3, 0.6, 0.9)

        assertEquals(expected.size, list.size)
        for (i in 0..<expected.size) {
            assertEquals(expected.getDouble(i), list.getDouble(i), 1.0e-6)
        }
    }

    @Test
    fun testFloatStepWithEpsilon() {
        val list = (0f..1f).step(0.3f)
        val expected = floatListOf(0f, 0.3f, 0.6f, 0.9f)
        assertEquals(expected.size, list.size)
        for (i in 0..<expected.size) {
            assertEquals(expected.getFloat(i), list.getFloat(i), 1.0e-6f)
        }
    }

    @Test
    fun testEmptyRange() {
        val doubleList = (1.0..0.0).step(0.1)
        assertTrue(doubleList.isEmpty())

        val floatList = (1f..0f).step(0.1f)
        assertTrue(floatList.isEmpty())
    }

    @Test
    fun testInvalidStep() {
        assertFailsWith<IllegalArgumentException> { (0.0..1.0).step(0.0) }
        assertFailsWith<IllegalArgumentException> { (0.0..1.0).step(-0.1) }

        assertFailsWith<IllegalArgumentException> { (0f..1f).step(0f) }
        assertFailsWith<IllegalArgumentException> { (0f..1f).step(-0.1f) }
    }

    @Test
    fun testNonFiniteRange() {
        assertFailsWith<IllegalArgumentException> { (Double.NaN..1.0).step(0.1) }
        assertFailsWith<IllegalArgumentException> { (0.0..Double.POSITIVE_INFINITY).step(0.1) }

        assertFailsWith<IllegalArgumentException> { (Float.NaN..1f).step(0.1f) }
        assertFailsWith<IllegalArgumentException> { (0f..Float.POSITIVE_INFINITY).step(0.1f) }
    }

    @Test
    fun testReadOnly() {
        val doubleList = (0.0..1.0).step(0.5)
        assertFailsWith<UnsupportedOperationException> { doubleList.add(2.0) }

        val floatList = (0f..1f).step(0.5f)
        assertFailsWith<UnsupportedOperationException> { floatList.add(2f) }
    }
}

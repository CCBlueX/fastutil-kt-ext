package net.ccbluex.fastutil

import it.unimi.dsi.fastutil.objects.Object2DoubleFunction
import it.unimi.dsi.fastutil.objects.Object2DoubleLinkedOpenHashMap
import org.junit.jupiter.api.assertThrows
import java.util.function.UnaryOperator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WeightedSortedListBlackBoxTest {

    private fun makeWeighter(map: Map<String, Double>): Object2DoubleFunction<String> = Object2DoubleLinkedOpenHashMap(map)

    @Test
    fun `add should insert in sorted position when weight in bounds`() {
        val weights = mapOf("a" to 1.0, "b" to 2.0, "c" to 1.5)
        val w = makeWeighter(weights)
        val list = WeightedSortedList<String>(defaultCapacity = 0, lowerBound = 1.0, lowerBoundInclusive = true, upperBound = 2.0, upperBoundInclusive = true, weighter = w)

        assertTrue(list.add("a"))
        assertTrue(list.add("b"))
        // c has weight 1.5 and should be inserted between a(1.0) and b(2.0)
        assertTrue(list.add("c"))
        assertEquals(listOf("a", "c", "b"), list.toList())
    }

    @Test
    fun `add should return false when weight out of bounds`() {
        val weights = mapOf("x" to 0.5)
        val w = makeWeighter(weights)
        val list = WeightedSortedList<String>(0, lowerBound = 1.0, lowerBoundInclusive = true, upperBound = 2.0, upperBoundInclusive = true, weighter = w)

        assertFalse(list.add("x"))
        assertTrue(list.isEmpty())
    }

    @Test
    fun `add at index should check bounds and order`() {
        val weights = mapOf("a" to 1.0, "b" to 2.0, "c" to 1.5)
        val w = makeWeighter(weights)
        val list = WeightedSortedList<String>(0, 1.0, true, 2.0, true, w)

        list.add("a")
        list.add("b")

        // inserting c(1.5) at index 1 is valid
        list.add(1, "c")
        assertEquals(listOf("a", "c", "b"), list.toList())

        // attempting to insert an out-of-bounds element should throw
        val weights2 = mapOf("z" to 3.0)
        val w2 = makeWeighter(weights2)
        val list2 = WeightedSortedList<String>(0, 1.0, true, 2.0, true, w2)
        assertThrows<IllegalStateException> { list2.add(0, "z") }

        // inserting an element that breaks sorted order should throw
        val weights3 = mapOf("d" to 1.2)
        val w3 = makeWeighter(weights3)
        val list3 = WeightedSortedList<String>(0, 1.0, true, 2.0, true, makeWeighter(mapOf("a" to 1.0, "b" to 2.0)))
        list3.add("a")
        list3.add("b")
        assertThrows<IllegalStateException> { list3.add(0, "d") } // d(1.2) cannot be placed at index 0
    }

    @Test
    fun `addAll collection should add elements individually and respect bounds`() {
        val map = mapOf("a" to 1.0, "b" to 1.1, "c" to 2.0)
        val w = makeWeighter(map)
        val list = WeightedSortedList<String>(0, 1.0, true, 2.0, true, w)

        assertTrue(list.addAll(listOf("a", "b")))
        assertFalse(list.addAll(emptyList()))
        // c is in bounds so adding multiple including c should succeed
        assertTrue(list.addAll(listOf("c")))
        assertEquals(listOf("a", "b", "c"), list.toList())
    }

    @Test
    fun `addAll at index requires contiguous non-decreasing block and neighbor fit`() {
        val map = mapOf("a" to 1.0, "b" to 1.2, "c" to 1.3, "d" to 2.0)
        val w = makeWeighter(map)
        val list = WeightedSortedList<String>(0, 1.0, true, 2.0, true, w)

        list.add("a")
        list.add("d")

        // valid block [b, c] fits between a and d
        assertTrue(list.addAll(1, listOf("b", "c")))
        assertEquals(listOf("a", "b", "c", "d"), list.toList())

        // invalid block (decreasing weights) should return false
        val badBlock = listOf("c", "b")
        assertFalse(list.addAll(1, badBlock))
    }

    @Test
    fun `addElements raw array variant throws on null and enforces order and bounds`() {
        val map = mapOf("a" to 1.0, "b" to 1.2)
        val w = makeWeighter(map)
        val list = WeightedSortedList<String>(0, 1.0, true, 2.0, true, w)

        // null inside array should throw NullPointerException
        val arrWithNull: Array<String?> = arrayOf("a", null)
        assertThrows<NullPointerException> { list.addElements(0, arrWithNull, 0, arrWithNull.size) }

        // valid insertion
        val arr: Array<String?> = arrayOf("a", "b")
        list.add(0, "a") // ensure neighbor checks: insert another block at index 1 fails if it doesn't fit
        // inserting [b] at index 1 should be fine
        list.addElements(1, arr, 1, 1) // insert "b"
        assertEquals(listOf("a", "b"), list.toList())
    }

    @Test
    fun `remove and removeAt and removeIf and bulk operations`() {
        val map = mapOf("a" to 1.0, "b" to 1.5, "c" to 1.8)
        val w = makeWeighter(map)
        val list = WeightedSortedList<String>(0, 1.0, true, 2.0, true, w)

        list.addAll(listOf("a", "b", "c"))
        assertTrue(list.remove("b"))
        assertEquals(listOf("a", "c"), list.toList())

        val removed = list.removeAt(1)
        assertEquals("c", removed)
        assertEquals(listOf("a"), list.toList())

        // add again and use removeIf
        list.addAll(listOf("b", "c"))
        val changed = list.removeIf(java.util.function.Predicate { s -> s.startsWith("b") })
        assertTrue(changed)
        assertFalse(list.contains("b"))

        // removeAll / retainAll
        list.addAll(listOf("b", "c"))
        assertTrue(list.removeAll(listOf("a", "c")))
        assertEquals(listOf("b"), list.toList())
        assertTrue(list.retainAll(listOf()))
    }

    @Test
    fun `set enforces bounds and neighbor order`() {
        val map = mapOf("a" to 1.0, "b" to 1.5, "c" to 1.8, "x" to 0.5)
        val w = makeWeighter(map)
        val list = WeightedSortedList<String>(0, 1.0, true, 2.0, true, w)
        list.addAll(listOf("a", "b", "c"))

        // setting with out-of-bounds weight should throw
        assertThrows<IllegalStateException> { list.set(1, "x") }

        // setting to an element that violates neighbor order should throw
        val map2 = mapOf("y" to 1.9)
        val w2 = makeWeighter(map2)
        val list2 = WeightedSortedList<String>(0, 1.0, true, 2.0, true, makeWeighter(map + map2))
        list2.addAll(listOf("a", "b", "c"))
        // try to set index 1 to y(1.9) which is > right neighbor c(1.8)
        assertThrows<IllegalStateException> { list2.set(1, "y") }
    }

    @Test
    fun `replaceAll verifies bounds and non-decreasing property`() {
        val map = mapOf("a" to 1.0, "b" to 1.5, "c" to 1.8, "r1" to 1.6, "r2" to 1.4)
        val w = makeWeighter(map)
        val list = WeightedSortedList<String>(0, 1.0, true, 2.0, true, w)
        list.addAll(listOf("a", "b", "c"))

        // operator that keeps order
        list.replaceAll(UnaryOperator { s -> if (s == "a") "a" else s })
        assertEquals(3, list.size)

        // operator that produces an out-of-bounds weight
        val mapOut = mapOf("oob" to 2.5)
        val wOut = makeWeighter(map + mapOut)
        val listOut = WeightedSortedList<String>(0, 1.0, true, 2.0, true, wOut)
        listOut.addAll(listOf("a", "b", "c"))
        assertThrows<IllegalStateException> { listOut.replaceAll(UnaryOperator { _ -> "oob" }) }

        // operator that results in non-decreasing violation
        val listBad = WeightedSortedList<String>(0, 1.0, true, 2.0, true, w)
        listBad.addAll(listOf("a", "b", "c"))
        assertThrows<IllegalStateException> {
            listBad.replaceAll(
                UnaryOperator { s ->
                    if (s == "a") {
                        "b"
                    } else if (s == "b") {
                        "a"
                    } else {
                        s
                    }
                },
            )
        }
    }

    @Test
    fun `iterator and listIterator behavior including IllegalStateExceptions`() {
        val map = mapOf("a" to 1.0, "b" to 1.2)
        val w = makeWeighter(map)
        val list = WeightedSortedList<String>(0, 1.0, true, 2.0, true, w)
        list.addAll(listOf("a", "b"))

        val it = list.listIterator()
        // BEFORE calling next(): lastRet == -1, so remove() and set(...) must throw
        assertThrows<IllegalStateException> { it.remove() }
        assertThrows<IllegalStateException> { it.set("a") }

        // advance iterator: next() sets lastRet, so remove/set become valid
        assertTrue(it.hasNext())
        assertEquals("a", it.next())
        // set should be allowed now (weight of "a" fits)
        it.set("a")
        // remove should be allowed now
        it.remove()
        // second remove without intervening next/set should throw
        assertThrows<IllegalStateException> { it.remove() }

        // test iterator.add respects order
        val mapExtended = map + ("m" to 1.1)
        val listExt = WeightedSortedList<String>(0, 1.0, true, 2.0, true, makeWeighter(mapExtended))
        listExt.addAll(listOf("a", "b"))
        val it3 = listExt.listIterator(1)
        it3.add("m")
        assertEquals(listOf("a", "m", "b"), listExt.toList())
    }

    @Test
    fun `getDouble and defaultReturnValue behavior`() {
        val map = mapOf("a" to 1.0)
        val w = makeWeighter(map)
        val list = WeightedSortedList<String>(0, 0.0, true, 2.0, true, w)
        list.add("a")
        assertEquals(1.0, list.getDouble("a"))
        // default return value when absent
        list.defaultReturnValue(-1.0)
        assertEquals(-1.0, list.getDouble("nope"))
    }

    @Test
    fun `equals hashCode toString and unsupported operations`() {
        val map = mapOf("a" to 1.0, "b" to 1.2)
        val w = makeWeighter(map)
        val l1 = WeightedSortedList<String>(0, 1.0, true, 2.0, true, w)
        val l2 = WeightedSortedList<String>(0, 1.0, true, 2.0, true, w)
        l1.addAll(listOf("a", "b"))
        l2.addAll(listOf("a", "b"))
        assertEquals(l1, l2)
        assertEquals(l1.hashCode(), l2.hashCode())
        // toString contains elements and weights
        val s = l1.toString()
        assertTrue(s.contains("a") && s.contains("1.0"))

        // sort is unsupported
        assertThrows<UnsupportedOperationException> { (l1 as java.util.List<*>).sort(null) }
    }
}

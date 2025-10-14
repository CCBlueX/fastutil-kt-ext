package net.ccbluex.fastutil

import net.ccbluex.fastutil.Pool.Companion.use
import kotlin.test.*
import java.util.concurrent.atomic.AtomicInteger

class PoolTest {

    // Test basic borrow and recycle functionality
    @Test
    fun `should borrow and recycle objects correctly`() {
        // Create a counter to track object creation
        val createCounter = AtomicInteger(0)

        // Create a string builder pool
        val pool = Pool {
            createCounter.incrementAndGet()
            StringBuilder()
        }

        // First borrow should create a new object
        val first = pool.borrow()
        assertEquals(1, createCounter.get())

        // After recycling, borrowing again should get the same object
        first.append("test")
        pool.recycle(first)
        val recycled = pool.borrow()
        assertEquals(1, createCounter.get())
        assertEquals("test", recycled.toString())
    }

    // Test object pool with reset functionality
    @Test
    fun `should reset objects when recycled with finalizer`() {
        // Create a string builder pool with a finalizer
        val pool = Pool({
            StringBuilder()
        }, {
            it.clear() // Clear the builder when recycled
        })

        val builder = pool.borrow()
        builder.append("content")
        assertEquals("content", builder.toString())

        pool.recycle(builder)
        val recycled = pool.borrow()
        assertEquals("", recycled.toString()) // Should be empty after reset
    }

    // Test batch recycle functionality
    @Test
    fun `should recycle multiple objects at once`() {
        val createCounter = AtomicInteger(0)
        val pool = Pool {
            createCounter.incrementAndGet()
            StringBuilder()
        }

        // Borrow multiple objects
        val builders = listOf(
            pool.borrow(),
            pool.borrow(),
            pool.borrow()
        )
        assertEquals(3, createCounter.get())

        // Recycle all at once
        pool.recycleAll(builders)

        // Borrow again, should reuse previous objects
        val recycled1 = pool.borrow()
        val recycled2 = pool.borrow()
        val recycled3 = pool.borrow()
        assertEquals(3, createCounter.get())

        // Verify we got the same objects back (identity comparison)
        val allRecycled = listOf(recycled1, recycled2, recycled3).all { recycledBuilder ->
            builders.any { it === recycledBuilder }
        }
        assertTrue(allRecycled)
    }

    // Test the use extension function for automatic recycling
    @Test
    fun `should automatically recycle object after use`() {
        val createCounter = AtomicInteger(0)
        val pool = Pool {
            createCounter.incrementAndGet()
            StringBuilder()
        }

        // Use the use function
        val result = pool.use { builder ->
            builder.append("test")
            builder.toString()
        }

        assertEquals("test", result)

        // Borrow again, should reuse the object
        val recycled = pool.borrow()
        assertEquals(1, createCounter.get())
        assertEquals("test", recycled.toString())
    }

    // Test that objects are recycled even when an exception occurs
    @Test
    fun `should recycle object even when exception occurs in use block`() {
        val createCounter = AtomicInteger(0)
        val pool = Pool {
            createCounter.incrementAndGet()
            StringBuilder()
        }

        // Borrow and recycle once to ensure the pool has objects
        val initial = pool.borrow()
        pool.recycle(initial)

        // Throw an exception in the use block
        assertFailsWith<RuntimeException> {
            pool.use { builder ->
                builder.append("will fail")
                throw RuntimeException("Test exception")
            }
        }

        // Even with exception, the object should be recycled
        val recycledAfterException = pool.borrow()
        assertEquals(1, createCounter.get())
        assertEquals("will fail", recycledAfterException.toString())
    }

    // Test pool behavior under multiple borrow-recycle cycles
    @Test
    fun `should handle multiple borrow and recycle cycles`() {
        val createCounter = AtomicInteger(0)
        val pool = Pool(initializer = {
            createCounter.incrementAndGet()
            mutableListOf<Int>()
        }, MutableList<*>::clear)

        // Perform multiple borrow-use-recycle cycles
        for (i in 0 until 100) {
            val list = pool.borrow()
            list.add(i)
            assertEquals(1, list.size)
            pool.recycle(list)
        }

        // Since we're recycling, only a few objects should be created
        assertTrue(createCounter.get() <= 10) // Should need at most 10 objects
    }


    // Test batch borrowing of objects
    @Test
    fun `should borrow multiple objects into collection`() {
        val createCounter = AtomicInteger(0)
        val pool = Pool {
            createCounter.incrementAndGet()
            StringBuilder()
        }

        // Prepare destination collection
        val destination = mutableListOf<StringBuilder>()

        // Borrow objects into collection
        pool.borrowInto(destination, 5)

        // Should have created 5 new objects
        assertEquals(5, createCounter.get())
        assertEquals(5, destination.size)

        // All objects should be distinct instances
        val distinctCount = destination.distinctBy { it.hashCode() }.size
        assertEquals(5, distinctCount)
    }

    // Test borrowInto when pool has recycled objects
    @Test
    fun `should reuse recycled objects when using borrowInto`() {
        val createCounter = AtomicInteger(0)
        val pool = Pool {
            createCounter.incrementAndGet()
            StringBuilder()
        }

        // Create and recycle some objects first
        val initialObjects = List(3) { pool.borrow() }
        initialObjects.forEach { it.append("recycled") }
        pool.recycleAll(initialObjects)

        // Borrow using borrowInto
        val destination = mutableListOf<StringBuilder>()
        pool.borrowInto(destination, 5)

        // Should have created only 2 new objects (3 from pool + 2 new)
        assertEquals(5, createCounter.get())
        assertEquals(5, destination.size)

        // At least 3 objects should have the recycled content
        val recycledContentCount = destination.count { it.toString() == "recycled" }
        assertEquals(3, recycledContentCount)
    }

    // Test clear functionality
    @Test
    fun `should clear all objects in pool`() {
        val createCounter = AtomicInteger(0)
        val pool = Pool {
            createCounter.incrementAndGet()
            StringBuilder()
        }

        // Create and recycle objects
        val objects = List(5) { pool.borrow() }
        pool.recycleAll(objects)

        // Clear the pool
        pool.clear()

        // Next borrow should create new objects
        val newObject = pool.borrow()
        assertFalse(objects.contains(newObject))
    }

    // Test clearInto functionality
    @Test
    fun `should clear objects into collection and return correct count`() {
        val pool = Pool {
            StringBuilder()
        }

        // Create and recycle objects with unique content
        for (i in 0 until 5) {
            val builder = StringBuilder()
            builder.append("item$i")
            pool.recycle(builder)
        }

        // Clear into destination collection
        val destination = mutableListOf<StringBuilder>()
        val clearedCount = pool.clearInto(destination)

        // Should have returned 5 objects
        assertEquals(5, clearedCount)
        assertEquals(5, destination.size)

        // Verify all recycled objects are in the destination
        val expectedContents = (0 until 5).map { "item$it" }.toSet()
        val actualContents = destination.map { it.toString() }.toSet()
        assertEquals(expectedContents, actualContents)

        // Pool should now be empty
        val newObject = pool.borrow()
        assertFalse(destination.contains(newObject))
    }

    // Test borrowInto with count of zero
    @Test
    fun `should handle borrowInto with zero count`() {
        val pool = Pool { StringBuilder() }
        val destination = mutableListOf<StringBuilder>()

        // Calling with zero should not add any objects
        pool.borrowInto(destination, 0)
        assertTrue(destination.isEmpty())
    }

    // Test borrowInto with negative count should throw exception
    @Test
    fun `should throw exception when borrowInto with negative count`() {
        val pool = Pool { StringBuilder() }
        val destination = mutableListOf<StringBuilder>()

        assertFailsWith<IllegalArgumentException> {
            pool.borrowInto(destination, -1)
        }
    }
}

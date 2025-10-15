package net.ccbluex.fastutil

import it.unimi.dsi.fastutil.objects.ReferenceArrayList
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * An object pool for reusing mutable objects to reduce garbage collection overhead.
 *
 * @param E Type of objects to be pooled
 */
sealed interface Pool<E : Any> {

    /**
     * Borrow an object from the pool. If pool is empty, a new object is created.
     * @return The borrowed object
     */
    fun borrow(): E

    /**
     * Borrow [count] objects and add them into [destination].
     * The order of addition is not permitted.
     *
     * @param destination The collection to receive objects.
     * @param count The count of objects to borrow
     * @throws IllegalArgumentException if count is negative
     */
    fun borrowInto(destination: MutableCollection<E>, count: Int)

    /**
     * Recycle an object back to the pool for future reuse.
     * @param value The object to recycle
     */
    fun recycle(value: E)

    /**
     * Recycle multiple objects at once.
     * @param values Iterable of objects to recycle
     */
    fun recycleAll(values: Iterable<E>)

    /**
     * Clear all stored objects.
     *
     * @return The count of cleared objects
     */
    fun clear(): Int

    /**
     * Clear all stored objects and add them into [destination].
     * The order of addition is not permitted.
     *
     * @param destination The collection to receive objects.
     * @return The count of cleared objects
     */
    fun clearInto(destination: MutableCollection<E>): Int

    /**
     * Returns a thread-safe synchronized version of this pool.
     * @return Synchronized Pool instance
     */
    fun synchronized(): Pool<E> = Sync(this)

    companion object {
        /**
         * Creates a new object pool.
         *
         * @param initializer Supplier to create new objects when pool is empty
         * @return New Pool instance (not thread-safe)
         */
        @JvmStatic
        @JvmName("create")
        operator fun <E : Any> invoke(
            initializer: Supplier<E>,
        ) : Pool<E> = invoke(initializer) {}

        /**
         * Creates a new object pool.
         *
         * @param initializer Supplier to create new objects when pool is empty
         * @param finalizer Consumer to reset objects before reuse, should be idempotence, no side effect, and not throwing any exception.
         * @return New Pool instance
         */
        @JvmStatic
        @JvmName("create")
        operator fun <E : Any> invoke(
            initializer: Supplier<E>,
            finalizer: Consumer<E>,
        ) : Pool<E> = ListBasedPool(initializer, finalizer)

        /**
         * Scoped use of a pooled object. Automatically recycles the object after use.
         *
         * @param action Function to execute with the borrowed object
         * @return Result of the action
         */
        inline fun <E : Any, R> Pool<E>.use(action: (E) -> R): R {
            val value = borrow()
            try {
                return action(value)
            } finally {
                recycle(value)
            }
        }
    }

    /**
     * Thread-safe synchronized wrapper for any Pool implementation.
     */
    private class Sync<E : Any>(
        private val delegate: Pool<E>,
    ) : Pool<E> {
        @Synchronized
        override fun borrow() = delegate.borrow()
        @Synchronized
        override fun borrowInto(destination: MutableCollection<E>, count: Int) = delegate.borrowInto(destination, count)
        @Synchronized
        override fun recycle(value: E) = delegate.recycle(value)
        @Synchronized
        override fun recycleAll(values: Iterable<E>) = delegate.recycleAll(values)
        @Synchronized
        override fun clear() = delegate.clear()
        @Synchronized
        override fun clearInto(destination: MutableCollection<E>) = delegate.clearInto(destination)

        override fun synchronized(): Pool<E> = this
    }

    /**
     * Default Pool implementation backed by a list (stack) structure.
     */
    private class ListBasedPool<E : Any>(
        private val initializer: Supplier<E>,
        private val finalizer: Consumer<E>,
    ) : Pool<E> {
        // Internal storage for pooled objects
        private val stack = ReferenceArrayList<E>()

        private val batchBorrowBuffer = ReferenceArrayList<E>()

        override fun borrow(): E = if (stack.isEmpty) initializer.get() else stack.removeAt(stack.size - 1)

        override fun borrowInto(destination: MutableCollection<E>, count: Int) {
            if (count < 0) throw IllegalArgumentException("count (${count}) < 0")
            if (count == 0) return

            val size = stack.size
            if (count >= size) {
                destination.addAll(stack)
                stack.clear()
                when (val remaining = count - size) {
                    0 -> return
                    1 -> destination.add(initializer.get())
                    else -> {
                        batchBorrowBuffer.size(remaining)
                        repeat(remaining) { batchBorrowBuffer[it] = initializer.get() }
                        destination.addAll(batchBorrowBuffer)
                        batchBorrowBuffer.clear()
                    }
                }
            } else {
                batchBorrowBuffer.size(count)
                stack.getElements(size - count, batchBorrowBuffer.elements(), 0, count)
                stack.size(size - count)
                destination.addAll(batchBorrowBuffer)
                batchBorrowBuffer.clear()
            }
        }

        override fun recycle(value: E) {
            finalizer.accept(value)
            stack.add(value)
        }

        override fun recycleAll(values: Iterable<E>) {
            if (values is Collection) stack.ensureCapacity(stack.size + values.size)
            for (value in values) {
                finalizer.accept(value)
                stack.add(value)
            }
        }

        override fun clear(): Int {
            val n = stack.size
            stack.clear()
            return n
        }

        override fun clearInto(destination: MutableCollection<E>): Int {
            destination.addAll(stack)
            val n = stack.size
            stack.clear()
            return n
        }
    }

}
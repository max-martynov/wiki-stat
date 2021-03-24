package ru.senin.kotlin.wiki.workers

import java.util.*

class ClosedChannelException : IllegalStateException("Channel was closed")

class Channel<T> {
    private val lock = Object()

    @Volatile
    private var closed = false

    fun isClosed(): Boolean = synchronized(lock) {
        closed
    }

    val size: Int
        get() = synchronized(lock) {
            queue.size
        }

    private val queue: Queue<T> = LinkedList()

    fun add(elem: T) = synchronized(lock) {
        if (closed)
            throw ClosedChannelException()
        queue.add(elem)
        lock.notify()
    }

    fun addIfNotClosed(elem: T) = synchronized(lock) {
        if (!closed) {
            queue.add(elem)
            lock.notify()
        }
    }

    fun poll(): T? = synchronized(lock) {
        queue.poll()
    }

    fun waitingPoll() : T? = synchronized(lock) {
        while (!closed && queue.isEmpty())
            lock.wait()
        queue.poll()
    }

    fun close() = synchronized(lock) {
        closed = true
        lock.notifyAll()
    }

    operator fun iterator(): Iterator<T> =
        ChannelIterator(this)

    // I looked up channel implementation in the kotlinx coroutines
    class ChannelIterator<T>(private val channel: Channel<T>) : Iterator<T> {
        private var next : T? = null
        override fun hasNext(): Boolean {
            next = channel.waitingPoll()
            return next != null
        }

        override fun next(): T {
            if (next == null)
                hasNext()
            return next ?: throw ClosedChannelException()
        }
    }
}
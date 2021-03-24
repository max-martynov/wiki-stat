package ru.senin.kotlin.wiki.workers

import ru.senin.kotlin.wiki.data.*

import java.util.concurrent.BlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.FutureTask
import java.util.concurrent.atomic.AtomicBoolean

class DataWorker(
    private val channel: Channel<Page>,
    private val result: CompletableFuture<PageStats>
) : Runnable {
    private val stats = PageStats()
    private var closed = false

    override fun run() {
        for (page in channel)
            process(page)
        result.complete(stats)
    }

    private fun process(page: Page) {
        stats.consume(page)
    }
}
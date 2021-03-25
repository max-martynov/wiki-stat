package ru.senin.kotlin.wiki.workers

import ru.senin.kotlin.wiki.data.*

import java.util.concurrent.BlockingQueue
import java.util.concurrent.CompletableFuture

class DataWorker(
    private val queue: BlockingQueue<Page>,
    private val result: CompletableFuture<PageStats>
) : Runnable {
    private val stats = PageStats()

    override fun run() {
        try {
            while (true) {
                process(queue.take())
            }
        } catch (e: InterruptedException) {
            val pages = mutableListOf<Page>()
            queue.drainTo(pages)
            pages.forEach { page -> process(page) }
        } finally {
            result.complete(stats)
        }
    }

    private fun process(page: Page) {
        stats.consume(page)
    }
}
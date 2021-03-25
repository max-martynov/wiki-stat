package ru.senin.kotlin.wiki.workers

import ru.senin.kotlin.wiki.data.*
import ru.senin.kotlin.wiki.parser.Parser

import java.util.concurrent.BlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingDeque

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

fun processFile(parser: Parser, numberOfThreads: Int): PageStats {
    val results = List(numberOfThreads) {
        CompletableFuture<PageStats>()
    }
    val pagesQueue = LinkedBlockingDeque<Page>()
    parser.onPage { page ->
        pagesQueue.add(page)
    }

    val pool = results.map { result ->
        Thread(DataWorker(pagesQueue, result))
    }

    pool.forEach { it.start() }
    parser.parse()
    pool.forEach { it.interrupt() }

    return results
        .mapNotNull { it.get() }
        .mergeAll()
}
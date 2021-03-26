package ru.senin.kotlin.wiki.workers

import ru.senin.kotlin.wiki.data.*
import ru.senin.kotlin.wiki.parser.Bz2XMLParser
import ru.senin.kotlin.wiki.parser.Parser
import java.io.File
import java.util.concurrent.*

class PageParser(
        private val files: BlockingQueue<File>,
        private val pages: BlockingQueue<Page>,
): Runnable {
    override fun run() {
        while (!files.isEmpty()) {
            val file = files.take()
            processFile(file)
        }
    }

    private fun processFile(file: File) {
        val parser = Bz2XMLParser(file.inputStream())
        parser.onPage { page ->
            pages.add(page)
        }
        parser.parse()
    }
}

class PageWorker(
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

fun processFiles(files: List<File>, numberOfThreads: Int): PageStats {
    val results = List(numberOfThreads) {
        CompletableFuture<PageStats>()
    }
    val filesQueue = LinkedBlockingQueue(files)
    val pagesQueue = LinkedBlockingQueue<Page>()

    val parsersPool = List(numberOfThreads) {
        Thread(PageParser(filesQueue, pagesQueue))
    }
    parsersPool.forEach { it.start() }

    val workersPool = results.map { result ->
        Thread(PageWorker(pagesQueue, result))
    }
    workersPool.forEach { it.start() }

    parsersPool.forEach { it.join() }
    workersPool.forEach { it.interrupt() }

    return results
            .mapNotNull { it.get() }
            .mergeAll()
}

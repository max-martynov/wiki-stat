package ru.senin.kotlin.wiki.workers

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import ru.senin.kotlin.wiki.data.*
import ru.senin.kotlin.wiki.parser.Parser
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.util.concurrent.*

class PageParser(
    private val files: BlockingQueue<File>,
    private val pages: BlockingQueue<Page>,
    private val parserFactory: (InputStream) -> Parser
) : Runnable {
    override fun run() {
        while (!files.isEmpty()) {
            val file = files.take()
            processFile(file)
        }
    }

    private val bufferSize = 8192 * 24

    private fun processFile(file: File) {
        val parser = parserFactory(
            BZip2CompressorInputStream(
                BufferedInputStream(file.inputStream(), bufferSize)
            )
        )
        parser.onPage { page ->
            pages.add(page)
        }
        parser.parse()
    }
}

class PageWorker(
    private val queue: BlockingQueue<Page>,
    private val result: CompletableFuture<PageStats>,
    optimizations: Boolean
) : Runnable {
    private val stats = PageStats(optimizations)

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

fun processFiles(
    files: List<File>,
    parserPoolSize: Int,
    workerPoolSize: Int,
    parserFactory: (InputStream) -> Parser,
    optimizations: Boolean
): PageStats {
    val results = List(workerPoolSize) {
        CompletableFuture<PageStats>()
    }
    val filesQueue = LinkedBlockingQueue(files)
    val pagesQueue = LinkedBlockingQueue<Page>()

    val parsersPool = List(parserPoolSize) {
        Thread(PageParser(filesQueue, pagesQueue, parserFactory))
    }
    parsersPool.forEach { it.start() }

    val workersPool = results.map { result ->
        Thread(PageWorker(pagesQueue, result, optimizations))
    }
    workersPool.forEach { it.start() }

    parsersPool.forEach { it.join() }
    workersPool.forEach { it.interrupt() }

    return results
            .mapNotNull { it.get() }
            .mergeAll(optimizations)
}

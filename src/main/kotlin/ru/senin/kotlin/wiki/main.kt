package ru.senin.kotlin.wiki

import ru.senin.kotlin.wiki.data.*
import ru.senin.kotlin.wiki.parser.*
import ru.senin.kotlin.wiki.workers.*

import com.apurebase.arkenv.Arkenv
import com.apurebase.arkenv.argument
import com.apurebase.arkenv.parse
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingDeque
import kotlin.time.measureTime

class Parameters : Arkenv() {
    val inputs: List<File> by argument("--inputs") {
        description = "Path(s) to bzip2 archived XML file(s) with WikiMedia dump. Comma separated."
        mapping = {
            it.split(",").map{ name -> File(name) }
        }
        validate("File does not exist or cannot be read") {
            it.all { file -> file.exists() && file.isFile && file.canRead() }
        }
    }

    val output: String by argument("--output") {
        description = "Report output file"
        defaultValue = { "statistics.txt" }
    }

    val threads: Int by argument("--threads") {
        description = "Number of threads"
        defaultValue = { 4 }
        validate("Number of threads must be in 1..32") {
            it in 1..32
        }
    }
}

lateinit var parameters: Parameters

fun processFile(input: File, numberOfThreads: Int) : PageStats {
    val parser: Parser = Bz2XMLParser(input.inputStream())
    val futures = List(numberOfThreads) {
        CompletableFuture<PageStats>()
    }
    val pagesQueue = LinkedBlockingDeque<Page>()
    val pool = futures.map {
        Thread(DataWorker(pagesQueue, it))
    }
    parser.onPage { page ->
        pagesQueue.add(page)
    }

    pool.forEach { it.start() }
    parser.parse()
    pool.forEach { it.interrupt() }

    val result = PageStats()

    futures
        .mapNotNull { it.get() }
        .forEach { stats ->
            result.merge(stats)
        }

    return result
}

fun printResultToFile(result: PageStats, file: File) {
    val writer = file.writer()
    writer.write("Топ-300 слов в заголовках статей:\n")
    result.titleStats.getTopK(300).forEach { writer.write("${it.second} ${it.first}\n") }
    writer.write("\n")

    writer.write("Топ-300 слов в статьях:\n")
    result.bodyStats.getTopK(300).forEach { writer.write("${it.second} ${it.first}\n") }
    writer.write("\n")

    writer.write("Распределение статей по размеру:\n")
    val firstNotZeroSize = result.sizeStats.sizeCount.keys.minOrNull()
    val lastNotZeroSize = result.sizeStats.sizeCount.keys.maxOrNull()

    if (firstNotZeroSize != null && lastNotZeroSize != null)
        for (i in firstNotZeroSize..lastNotZeroSize)
            writer.write("$i ${result.sizeStats.sizeCount[i] ?: 0}\n")
    writer.write("\n")

    writer.write("Распределение статей по времени:\n")
    val firstNotZeroYear = result.yearStats.yearsAll.indexOfFirst { it != 0 }
    val lastNotZeroYear = result.yearStats.yearsAll.indexOfLast { it != 0 }

    if (firstNotZeroYear != -1)
        for (i in firstNotZeroYear..lastNotZeroYear)
            writer.write("${i + result.yearStats.startYear} ${result.yearStats.yearsAll[i]}\n")

    writer.close()
}

fun main(args: Array<String>) {
    try {
        parameters = Parameters().parse(args)

        if (parameters.help) {
            println(parameters.toString())
            return
        }

        val duration = measureTime {
            val stats = parameters.inputs.map { input ->
                processFile(input, parameters.threads)
            }.mergeAll()

            val file = File(parameters.output)
            printResultToFile(stats, file)
        }
        println("Time: ${duration.inMilliseconds} ms")

    }
    catch (e: Exception) {
        println("Error! ${e.message}")
        throw e
    }
}

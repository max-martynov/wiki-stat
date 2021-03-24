package ru.senin.kotlin.wiki

import ru.senin.kotlin.wiki.data.*
import ru.senin.kotlin.wiki.parser.*
import ru.senin.kotlin.wiki.workers.*

import com.apurebase.arkenv.Arkenv
import com.apurebase.arkenv.argument
import com.apurebase.arkenv.parse
import java.io.File
import java.util.concurrent.CompletableFuture
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

fun processFile(input: File, numberOfThreads: Int) {
    val parser: Parser = Bz2XMLParser(input.inputStream())
    val futures = List(numberOfThreads) {
        CompletableFuture<PageStats>()
    }
    val pagesChannel = Channel<Page>()
    val pool = futures.map {
        Thread(DataWorker(pagesChannel, it))
    }
    parser.onPage { page ->
        pagesChannel.add(page)
    }

    pool.forEach { it.start() }
    parser.parse()
    pagesChannel.close()

    val result = PageStats()

    futures
        .mapNotNull { it.get() }
        .forEach { stats ->
            result.merge(stats)
        }

    println(result.bodyStats.getTopK(300))
}

fun main(args: Array<String>) {
    try {
        parameters = Parameters().parse(args)

        if (parameters.help) {
            println(parameters.toString())
            return
        }

        val duration = measureTime {
            for (input in parameters.inputs) {
                processFile(input, parameters.threads)
            }
        }
        println("Time: ${duration.inMilliseconds} ms")

    }
    catch (e: Exception) {
        println("Error! ${e.message}")
        throw e
    }
}

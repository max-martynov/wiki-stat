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

    val optimizations: Boolean by argument("--optimizations") {
        description = "Do you need power of random?"
        mapping = {
            it.toBoolean()
        }
        defaultValue = { false }
    }
}

lateinit var parameters: Parameters

fun printResultToFile(result: PageStats, file: File) {
    val writer = file.writer()

    writer.write("Топ-300 слов в заголовках статей:\n")
    writer.write(result.titleStats.topKToString(300))
    writer.write("\n")

    writer.write("Топ-300 слов в статьях:\n")
    writer.write(result.bodyStats.topKToString(300))
    writer.write("\n")

    writer.write("Распределение статей по размеру:\n")
    writer.write(result.sizeStats.toString())
    writer.write("\n")

    writer.write("Распределение статей по времени:\n")
    writer.write(result.yearStats.toString())

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
            val stats = processFiles(parameters.inputs, parameters.threads)

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

package ru.senin.kotlin.wiki

import ru.senin.kotlin.wiki.data.*
import ru.senin.kotlin.wiki.parser.*
import ru.senin.kotlin.wiki.workers.*

import com.apurebase.arkenv.*
import java.io.File
import java.io.InputStream
import java.lang.IllegalArgumentException
import java.lang.Integer.min
import kotlin.time.measureTime

enum class ParserType {
    SAX, VTD
}

class Parameters : Arkenv() {
    val inputs: List<File> by argument("--inputs") {
        description = "Path(s) to bzip2 archived XML file(s) with WikiMedia dump. Comma separated."
        mapping = {
            it.split(",").map { name -> File(name) }
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

    val parserType: ParserType by argument("--parser", "-p") {
        description = "Choose preferred parser. " +
                "Available are: ${ParserType.values().joinToString { it.name }}"
        defaultValue = { ParserType.SAX }
        mapping = {
            when (it) {
                ParserType.SAX.name -> ParserType.SAX
                ParserType.VTD.name -> ParserType.VTD
                else -> throw IllegalArgumentException("Unsupported parser")
            }
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

fun createParserFactory(parserType: ParserType): (InputStream) -> Parser =
    when (parserType) {
        ParserType.SAX -> { input -> SAXParser(input) }
        ParserType.VTD -> { input -> VTDParser(input) }
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
    val maxParsingThreads = 8
    try {
        parameters = Parameters().parse(args)

        if (parameters.help) {
            println(parameters.toString())
            return
        }

        val duration = measureTime {
            val stats = processFiles(
                parameters.inputs,
                min(parameters.threads, maxParsingThreads),
                parameters.threads,
                createParserFactory(parameters.parserType),
                parameters.optimizations
            )

            val file = File(parameters.output)
            printResultToFile(stats, file)
        }
        println("Time: ${duration.inMilliseconds} ms")

    } catch (e: Exception) {
        println("Error! ${e.message}")
        throw e
    }
}

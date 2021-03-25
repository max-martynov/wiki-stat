package ru.senin.kotlin.wiki.data

import java.time.LocalDateTime
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.log
import kotlin.math.log10

private fun String.isRussianWord() =
    count { it in 'а'..'я' } >= 3

class WordStats {
    private val wordCnt: MutableMap<String, Int> = HashMap()

    // O(1)
    private fun add(word: String) {
        wordCnt[word] = wordCnt.getOrDefault(word, 0) + 1
    }

    infix fun merge(other: WordStats) {
        other.wordCnt.entries.forEach { (word, cnt) ->
            wordCnt[word] = wordCnt.getOrDefault(word, 0) + cnt
        }
    }

    // O(n * log n). To be optimized to O(n + k * log k) on average
    fun getTopK(k: Int): List<Pair<String, Int>> {
        val entries = wordCnt.entries
        return entries
            .map { it.toPair() }
            .sortedWith(
                compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first }
            )
            .take(k)
    }

    fun consume(str: String) {
        str.toLowerCase()
            .split(" ")
            .filter { it.isRussianWord() }
            .forEach { token ->
                token.split("[^а-я]".toRegex())
                    .filter { it.length >= 3 }
                    .forEach { add(it) }
            }
    }
}

class SizeStats {
    private val maxLogPageSize = 1000
    val sizeCount = HashMap<Int, Int>()

    fun consume(size: Long) {
        val logSize = size.toString().length - 1
        assert(logSize >= 0L)
        sizeCount[logSize] = sizeCount.getOrDefault(logSize, 0) + 1
    }

    infix fun merge(other: SizeStats) {
        other.sizeCount.entries.forEach { (size, cnt) ->
            sizeCount[size] = sizeCount.getOrDefault(size, 0) + cnt
        }
    }
}

class YearStats {
    val startYear = 2000
    val yearsAll = IntArray(LocalDateTime.now().year - startYear + 1)

    fun consume(year: Int) {
        yearsAll[year - startYear]++
    }

    infix fun merge(other: YearStats) {
        for (i in yearsAll.indices)
            yearsAll[i] += other.yearsAll[i]
    }
}

class PageStats {
    val titleStats = WordStats()
    val bodyStats = WordStats()
    val sizeStats = SizeStats()
    val yearStats = YearStats()

    fun consume(page: Page) {
        titleStats.consume(page.title)
        bodyStats.consume(page.contents.text)
        sizeStats.consume(page.contents.sizeBytes)
        yearStats.consume(page.timestamp.year)
    }

    infix fun merge(other: PageStats) {
        titleStats.merge(other.titleStats)
        bodyStats.merge(other.bodyStats)
        sizeStats.merge(other.sizeStats)
        yearStats.merge(other.yearStats)
    }
}

fun Iterable<PageStats>.mergeAll(): PageStats {
    val result = PageStats()
    forEach { result.merge(it) }
    return result
}
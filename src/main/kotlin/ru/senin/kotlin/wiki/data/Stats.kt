package ru.senin.kotlin.wiki.data

import java.time.LocalDateTime
import kotlin.collections.HashMap

private fun Char.isRussianLetter() =
    this in 'а'..'я' || this in 'А'..'Я'

private fun String.isRussianWord() =
    count { it.isRussianLetter() } >= 3

// returns [length] if element not found
private inline fun String.indexOfFirstSince(startIndex: Int, delimPredicate: (Char) -> Boolean): Int {
    for (i in startIndex until length)
        if (delimPredicate(get(i)))
            return i
    return length
}

private inline fun String.split(delimPredicate: (Char) -> Boolean): List<String> {
    if (isEmpty())
        return emptyList()
    val result = mutableListOf<String>()
    var curIndex = -1
    while (curIndex < length) {
        val nextIndex = this.indexOfFirstSince(curIndex + 1, delimPredicate)
        if (curIndex + 1 < nextIndex) {
            val substr = substring(curIndex + 1, nextIndex)
            result.add(substr)
        }
        curIndex = nextIndex
    }
    return result
}

class WordStats {
    private val wordCnt: MutableMap<String, Int> = HashMap()

    // O(1)
    private fun add(word: String) {
        val prev = wordCnt[word] ?: 0
        wordCnt[word] = prev + 1
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
        str
            .split(" ")
            .filter { it.isRussianWord() }
            .forEach { token ->
                token.split { !it.isRussianLetter() }
                    .filter { it.length >= 3 }
                    .forEach { add(it.toLowerCase()) }
            }
    }
}

class SizeStats {
    private val maxLogPageSize = 1000
    val sizeCount = IntArray(maxLogPageSize)

    fun consume(size: Long) {
        val logSize = size.toString().length - 1
        sizeCount[logSize]++
    }

    infix fun merge(other: SizeStats) {
        for (i in sizeCount.indices)
            sizeCount[i] += other.sizeCount[i]
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
package ru.senin.kotlin.wiki.data

import java.time.LocalDateTime
import java.util.*
import kotlin.Comparator
import kotlin.collections.HashMap
import kotlin.random.Random

abstract class WordStats {
    var wordCnt: MutableMap<String, Int> = HashMap()

    abstract fun add(word: String, delta: Int = 1)

    abstract infix fun merge(other: WordStats)

    abstract fun getTopK(k: Int): List<Pair<String, Int>>

    abstract fun consume(str: String)

    abstract fun topKToString(k: Int): String

}

class DeterminantWordStats: WordStats() {
    // O(1)
    override fun add(word: String, delta: Int) {
        val prev = wordCnt[word] ?: 0
        wordCnt[word] = prev + delta
    }

    override infix fun merge(other: WordStats) {
        other.wordCnt.entries.forEach { (word, cnt) ->
            add(word, cnt)
        }
    }

    // Optimized to O(n + k * log k) on average
    override fun getTopK(k: Int): List<Pair<String, Int>> {
        val list = wordCnt.entries.map { it.toPair() }.toMutableList()
        val comparator = compareBy<Pair<String, Int>> { -it.second }.then(compareBy { it.first })
        list.findKthElement(minOf(k, list.size), comparator)
        return list.take(k).sortedWith(comparator)
    }

    override fun consume(str: String) {
        str
            .split(" ")
            .filter { it.isRussianWord() }
            .forEach { token ->
                token.split { !it.isRussianLetter() }
                    .filter { it.length >= 3 }
                    .forEach { add(it.toLowerCase()) }
            }
    }

    override fun topKToString(k: Int): String =
        getTopK(k)
            .joinToString(separator = "") {
                "${it.second} ${it.first}\n"
            }
}

class RandomWordStats : WordStats() {
    private var cnt = 0
    private val maxCnt = 100000

    override fun add(word: String, delta: Int) {
        val prev = wordCnt[word] ?: 0
        wordCnt[word] = prev + delta
        cnt++
        if (cnt == maxCnt) {
            reduce()
            cnt = 0
        }
    }

    private fun reduce() {
        val list = wordCnt.entries.map { it.toPair() }.toMutableList()
        val comparator = compareBy<Pair<String, Int>> { -it.second }.then(compareBy { it.first })
        val half = list.findKthElement(minOf(maxCnt / 2, list.size), comparator)
        wordCnt = wordCnt.filterValues { it >= half!!.second } as MutableMap<String, Int>
    }

    override infix fun merge(other: WordStats) {
        other.wordCnt.entries.forEach { (word, cnt) ->
            add(word, cnt)
        }
    }

    // Optimized to O(n + k * log k) on average
    override fun getTopK(k: Int): List<Pair<String, Int>> {
        val list = wordCnt.entries.map { it.toPair() }.toMutableList()
        val comparator = compareBy<Pair<String, Int>> { -it.second }.then(compareBy { it.first })
        list.findKthElement(minOf(k, list.size), comparator)
        return list.take(k).sortedWith(comparator)
    }

    override fun consume(str: String) {
        str
                .split(" ")
                .filter { it.isRussianWord() }
                .forEach { token ->
                    token.split { !it.isRussianLetter() }
                            .filter { it.length >= 3 }
                            .forEach { add(it.toLowerCase()) }
                }
    }

    override fun topKToString(k: Int): String =
            getTopK(k)
                    .joinToString(separator = "") {
                        "${it.second} ${it.first}\n"
                    }
}

class SizeStats {
    private val maxLogPageSize = 1000
    private val sizeCount = IntArray(maxLogPageSize)

    fun consume(size: Long) {
        val logSize = size.toString().length - 1
        sizeCount[logSize]++
    }

    infix fun merge(other: SizeStats) {
        for (i in sizeCount.indices)
            sizeCount[i] += other.sizeCount[i]
    }

    override fun toString(): String {
        val firstNotZeroSize = sizeCount.indexOfFirst { it != 0 }
        val lastNotZeroSize = sizeCount.indexOfLast { it != 0 }

        return if (firstNotZeroSize != -1)
            sizeCount.mapIndexed { i, cnt -> "$i $cnt\n" }
                .subList(firstNotZeroSize, lastNotZeroSize + 1)
                .joinToString(separator = "")
        else ""
    }
}

class YearStats {
    private val startYear = 2000
    private val yearsAll = IntArray(LocalDateTime.now().year - startYear + 1)

    fun consume(year: Int) {
        yearsAll[year - startYear]++
    }

    infix fun merge(other: YearStats) {
        for (i in yearsAll.indices)
            yearsAll[i] += other.yearsAll[i]
    }

    override fun toString(): String {
        val firstNotZeroYear = yearsAll.indexOfFirst { it != 0 }
        val lastNotZeroYear = yearsAll.indexOfLast { it != 0 }

        return if (lastNotZeroYear != -1)
            yearsAll.mapIndexed { i, cnt -> "${i + startYear} $cnt\n" }
                .subList(firstNotZeroYear, lastNotZeroYear + 1)
                .joinToString(separator = "")
        else ""
    }
}

class PageStats(private val optimizations: Boolean) {
    var titleStats: WordStats
    var bodyStats: WordStats
    init {
        if (optimizations) {
            titleStats = DeterminantWordStats()
            bodyStats = RandomWordStats()
        }
        else {
            titleStats = DeterminantWordStats()
            bodyStats = DeterminantWordStats()
        }
    }
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

fun Iterable<PageStats>.mergeAll(optimizations: Boolean): PageStats {
    val result = PageStats(optimizations)
    forEach { result.merge(it) }
    return result
}

fun <T> MutableList<T>.findKthElement(k: Int, comparator: Comparator<T>): T? {
    if (isEmpty())
        return null
    return getKthElement(this, comparator, 0, this.size - 1, k)
}

private fun <T> getKthElement(list: MutableList<T>, comparator: Comparator<T>, l: Int, r: Int, k: Int): T {
    if (l == r) return list[l]
    val m = partition(list, comparator, l, r)
    if (m - l + 1 >= k)
        return getKthElement(list, comparator, l, m, k)
    return getKthElement(list, comparator, m + 1, r, k - (m - l + 1))
}

private fun <T> partition(list: MutableList<T>, comparator: Comparator<T>, l: Int, r: Int): Int {
    val pos = Random.nextInt(l, r + 1)
    val x = list[pos]
    list[l] = list[pos].also { list[pos] = list[l] }
    var i = l
    var j = r
    while (i <= j) {
        while (comparator.compare(list[i], x) < 0) i++
        while (comparator.compare(list[j], x) > 0) j--
        if (i >= j) break
        list[i] = list[j].also { list[j] = list[i] }
        i++.also { j-- }
    }
    return j
}

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
package ru.senin.kotlin.wiki.data

private fun Char.isRussianLetter() =
    this in 'а'..'я'

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
            .split("[^а-я]".toRegex())
            .filter { it.length >= 3 }
            .forEach { add(it) }
    }
}

class PageStats {
    val titleStats = WordStats()
    val bodyStats = WordStats()

    fun consume(page: Page) {
        titleStats.consume(page.title)
        bodyStats.consume(page.contents.text)
    }

    infix fun merge(other: PageStats) {
        titleStats.merge(other.titleStats)
        bodyStats.merge(other.bodyStats)
    }
}
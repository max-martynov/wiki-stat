package ru.senin.kotlin.wiki.data

import java.time.ZonedDateTime

data class PageContents(val sizeBytes: Long, val text: String)
data class Page(val title: String, val timestamp: ZonedDateTime, val contents: PageContents)

class PageBuilder {
    lateinit var title: String
    lateinit var timestamp: ZonedDateTime
    var contentsBytes: Long = 0
    lateinit var contentsText: String

    @Throws(UninitializedPropertyAccessException::class)
    fun build() =
        Page(title, timestamp, PageContents(contentsBytes, contentsText))

    fun buildOrNull() =
        try {
            build()
        } catch (e: UninitializedPropertyAccessException) {
            null
        }
}
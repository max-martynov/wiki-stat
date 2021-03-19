package ru.senin.kotlin.wiki.parser

import ru.senin.kotlin.wiki.data.Page
import ru.senin.kotlin.wiki.data.PageBuilder

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.InputStream
import org.xml.sax.*
import org.xml.sax.helpers.DefaultHandler
import javax.xml.parsers.SAXParserFactory
import java.text.ParseException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private class PageHandler(private val pageCallback: (Page) -> Unit) : DefaultHandler() {
    companion object {
        private const val PAGE_TAG = "page"
        private const val TIMESTAMP_TAG = "timestamp"
        private const val TITLE_TAG = "title"
        private const val TEXT_TAG = "text"
        private const val BYTES_ATTR = "bytes"
    }

    private var builder: PageBuilder? = null

    private var elementValue: String = ""

    override fun characters(ch: CharArray, start: Int, length: Int) {
        elementValue = String(ch, start, length)
    }

    override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
        when (qName) {
            PAGE_TAG -> builder = PageBuilder()
            TEXT_TAG -> {
                val bytes: Long? = attributes.getValue(BYTES_ATTR)?.toLongOrNull()
                if (bytes != null)
                    builder?.contentsBytes = bytes
                else
                    discardPage()
            }
        }
    }

    override fun endElement(uri: String, localName: String, qName: String) {
        when (qName) {
            TITLE_TAG -> builder?.title = elementValue
            TEXT_TAG -> builder?.contentsText = elementValue
            TIMESTAMP_TAG -> {
                try {
                    val timestamp = LocalDateTime.parse(
                        elementValue.dropLast(1), // dirty hack to comply with standard
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    )
                    builder?.timestamp = timestamp.atOffset(ZoneOffset.UTC).toZonedDateTime()
                } catch (e: ParseException) {
                    discardPage()
                }
            }
            PAGE_TAG -> {
                builder?.buildOrNull()?.let { pageCallback(it) }
            }
        }
    }

    private fun discardPage() {
        builder = null
    }
}

class Bz2XMLParser(private val inputStream: InputStream) : Parser {
    private val factory = SAXParserFactory.newInstance()
    private val saxParser = factory.newSAXParser()

    private lateinit var pageCallback: (Page) -> Unit

    private fun unpack() =
        BZip2CompressorInputStream(BufferedInputStream(inputStream))

    override fun parse() {
        saxParser.parse(unpack(), PageHandler(pageCallback))
    }

    override fun onPage(callback: (Page) -> Unit) {
        pageCallback = callback
    }
}
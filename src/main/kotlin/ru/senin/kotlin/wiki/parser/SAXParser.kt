package ru.senin.kotlin.wiki.parser

import ru.senin.kotlin.wiki.data.Page
import ru.senin.kotlin.wiki.data.PageBuilder

import java.io.InputStream
import org.xml.sax.*
import org.xml.sax.helpers.DefaultHandler
import javax.xml.parsers.SAXParserFactory
import java.text.ParseException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.io.File

private class PageHandler(private val pageCallback: (Page) -> Unit) : DefaultHandler() {
    companion object {
        private const val PAGE_TAG = "page"
        private const val TIMESTAMP_TAG = "timestamp"
        private const val TITLE_TAG = "title"
        private const val TEXT_TAG = "text"
        private const val BYTES_ATTR = "bytes"
    }

    private var builder: PageBuilder? = null

    private var collectText = false
    private val rowsList = mutableListOf<String>()

    override fun characters(ch: CharArray, start: Int, length: Int) {
        if (collectText)
            rowsList.add(String(ch, start, length))
    }

    override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
        when (qName) {
            PAGE_TAG -> builder = PageBuilder()
            TEXT_TAG -> {
                collectText = true
                val bytes: Long? = attributes.getValue(BYTES_ATTR)?.toLongOrNull()
                if (bytes != null)
                    builder?.contentsBytes = bytes
                else
                    discardPage()
            }
            TITLE_TAG -> collectText = true
            TIMESTAMP_TAG -> collectText = true
        }
    }

    override fun endElement(uri: String, localName: String, qName: String) {
        val elementValue = rowsList.joinToString("")
        rowsList.clear()
        collectText = false

        when (qName) {
            TITLE_TAG -> builder?.title = elementValue
            TEXT_TAG -> builder?.contentsText = elementValue
            TIMESTAMP_TAG -> {
                try {
                    val timestamp = LocalDateTime.parse(
                        elementValue.trim().dropLast(1), // dirty hack to comply with standard
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

class SAXParser(private val inputStream: InputStream) : Parser {
    companion object {
        const val JAXP_SCHEMA_LANGUAGE =
            "http://java.sun.com/xml/jaxp/properties/schemaLanguage"
        const val W3C_XML_SCHEMA =
            "http://www.w3.org/2001/XMLSchema"
        const val JAXP_SCHEMA_SOURCE =
            "http://java.sun.com/xml/jaxp/properties/schemaSource"
    }

    private val factory = SAXParserFactory.newInstance().also {
        it.isValidating = true
    }

    private val saxParser = factory.newSAXParser().also {
        val scheme = File(javaClass.classLoader.getResource("schema.xsd")?.toURI()!!)
        it.setProperty(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA)
        it.setProperty(JAXP_SCHEMA_SOURCE, scheme)
    }

    private lateinit var pageCallback: (Page) -> Unit

    override fun parse() {
        saxParser.parse(inputStream, PageHandler(pageCallback))
    }

    override fun onPage(callback: (Page) -> Unit) {
        pageCallback = callback
    }
}

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
import javax.xml.XMLConstants
import javax.xml.validation.SchemaFactory
import kotlin.system.exitProcess

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

    override fun error(e: SAXParseException) {
        println("gg wp")
        throw e
    }

    private fun discardPage() {
        builder = null
    }
}

class SAXParser(private val inputStream: InputStream) : Parser {

    private val factory = SAXParserFactory.newInstance().also {
        val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        val scheme = schemaFactory.newSchema(javaClass.classLoader.getResource("schema.xsd"))
        it.schema = scheme
    }

    private val saxParser = factory.newSAXParser()

    private class MyErrorHandler : ErrorHandler {
        override fun warning(p0: SAXParseException?) {
            println("Warning!")
        }

        override fun error(p0: SAXParseException?) {
            println("Warning!!")
            throw SAXException("Error!")
        }

        override fun fatalError(p0: SAXParseException?) {
            println("Warning!!!")
            throw SAXException("Fatal error!")
        }
    }

    private lateinit var pageCallback: (Page) -> Unit

    override fun parse() {
        saxParser.parse(inputStream, PageHandler(pageCallback))
    }

    override fun onPage(callback: (Page) -> Unit) {
        pageCallback = callback
    }
}

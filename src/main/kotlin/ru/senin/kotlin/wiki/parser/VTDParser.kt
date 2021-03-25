package ru.senin.kotlin.wiki.parser

import com.ximpleware.*
import com.ximpleware.extended.*
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.utils.IOUtils
import ru.senin.kotlin.wiki.data.Page
import ru.senin.kotlin.wiki.data.PageBuilder
import ru.senin.kotlin.wiki.data.PageContents
import java.io.BufferedInputStream
import java.io.InputStream
import java.text.ParseException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


class VTDParser(private val inputStream: InputStream) : Parser {
    private val vg = VTDGenHuge()

    private val bufferSize = 8192 * 8

    private lateinit var pageCallback: (Page) -> Unit

    private fun unpack() =
            BZip2CompressorInputStream(BufferedInputStream(inputStream, bufferSize))

    override fun parse() {
        parseXmlUsingVtd(unpack())
    }

    override fun onPage(callback: (Page) -> Unit) {
        pageCallback = callback
    }

    private fun parseXmlUsingVtd(inputStream: InputStream) {
//        val xg = XMLMemMappedBuffer()
//        vg.setDoc(inputStream)
//        vg.parseFile()
//        vg.setDoc(IOUtils.toByteArray(inputStream))
        vg.parse(false)
        val vn = vg.nav
        val ap = AutoPilotHuge(vn)
        ap.selectXPath("/mediawiki")
        while (ap.evalXPath() != -1) {
            if(vn.toElement(VTDNav.FIRST_CHILD,"page")){
                do {
                    try {
                        vn.toElement(VTDNav.FIRST_CHILD, "title")
                        val title = vn.toNormalizedString(vn.text)

                        vn.toElement(VTDNav.PARENT)
                        vn.toElement(VTDNav.FIRST_CHILD, "revision")

                        vn.toElement(VTDNav.FIRST_CHILD, "timestamp")
                        val timestamp = LocalDateTime.parse(
                            vn.toNormalizedString(vn.text).trim().dropLast(1),
                            DateTimeFormatter.ISO_LOCAL_DATE_TIME
                        ).atOffset(ZoneOffset.UTC).toZonedDateTime()

                        vn.toElement(VTDNav.PARENT)

                        vn.toElement(VTDNav.FIRST_CHILD, "text")
                        val bytes = vn.toNormalizedString(vn.getAttrVal("bytes")).toLong()

                        val text = vn.toNormalizedString(vn.text)

                        pageCallback(Page(title, timestamp, PageContents(bytes, text)))

                        vn.toElement(VTDNav.PARENT)
                        vn.toElement(VTDNav.PARENT)
                    } catch (e: Exception) { }

                } while (vn.toElement(VTDNav.NEXT_SIBLING,"page"))
            }
        }
    }

}

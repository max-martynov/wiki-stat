package ru.senin.kotlin.wiki.parser

import com.ximpleware.*
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.utils.IOUtils
import ru.senin.kotlin.wiki.data.Page
import java.io.BufferedInputStream
import java.io.InputStream


class VTDParser(private val inputStream: InputStream) : Parser {
    var vtg = VTDGen() // Instantiate VTDGen

    var xmm = XMLModifier() //Instantiate XMLModifier
    private val bufferSize = 8192 * 8

    private fun unpack() =
            BZip2CompressorInputStream(BufferedInputStream(inputStream, bufferSize))

    override fun parse() {
        vtg.setDoc(IOUtils.toByteArray(inputStream))
        vtg
    }

    override fun onPage(callback: (Page) -> Unit) {
        TODO("Not yet implemented")
    }
}

object VTDXmlTest {
    @Throws(XPathParseException::class, XPathEvalException::class, NavException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        parseXmlUsingVtd("src/test/resources/testData/big.xml")
    }

    @Throws(XPathParseException::class, XPathEvalException::class, NavException::class)
    fun parseXmlUsingVtd(xmlFilePath: String?) {
        val vg = VTDGen()
        vg.parseFile(xmlFilePath, false)
        val vn = vg.nav
        val ap = AutoPilot(vn)
        ap.selectXPath("/mediawiki/page")
        while (ap.evalXPath() != -1) {
            vn.toElement(VTDNav.FIRST_CHILD, "title")
            toNormalizedStringText("title", vn)
            vn.toElement(VTDNav.PARENT)
            vn.toElement(VTDNav.FIRST_CHILD, "revision")
            vn.toElement(VTDNav.FIRST_CHILD, "timestamp")
            toNormalizedStringText("timestamp", vn)
            vn.toElement(VTDNav.PARENT)
            vn.toElement(VTDNav.FIRST_CHILD, "text")
            toNormalizedStringAttr("bytes", vn)
            toNormalizedStringText("text", vn)
        }
    }

    @Throws(NavException::class)
    private fun toNormalizedStringAttr(attrbName: String, vn: VTDNav): String? {
        return toNormalizedString(attrbName, vn.getAttrVal(attrbName), vn)
    }

    @Throws(NavException::class)
    private fun toNormalizedStringText(tagName: String, vn: VTDNav): String? {
        return toNormalizedString(tagName, vn.text, vn)
    }

    @Throws(NavException::class)
    private fun toNormalizedString(name: String, `val`: Int, vn: VTDNav): String? {
        var strValue: String? = null
        if (`val` != -1) {
            strValue = vn.toNormalizedString(`val`)
            println("$name:: $strValue")
        }
        return strValue
    }
}
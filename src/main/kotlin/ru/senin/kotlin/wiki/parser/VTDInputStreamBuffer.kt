package ru.senin.kotlin.wiki.parser

import com.ximpleware.extended.EOFExceptionHuge
import com.ximpleware.extended.IByteBuffer
import com.ximpleware.extended.ParseExceptionHuge
import java.io.*
import java.lang.Integer.min
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel


/**
 * Shamelessly stolen from [com.ximpleware.extended.XMLMemMappedBuffer]
 */
class VTDInputStreamBuffer(private val inputStream: InputStream) : IByteBuffer {
    private val length = inputStream.available().toLong()
    override fun length(): Long = (1L shl 33) // 8G

    private val firstPage = ByteArray(firstPageSize)
    private var prevPage = ByteArray(pageSize)
    private var curPage = ByteArray(pageSize)
    private var pageShift = -1
    private var curPageSize = 0

    override fun byteAt(index: Long): Byte {
        val page = (index shr pageSizePower).toInt()
        val ind = (index and pageSizeLowerBits).toInt()

        if (page < pageShift - 1) {
            if (page == 0)
                return firstPage[ind]
            else
                throw IllegalArgumentException("Requested byte was too far ago")
        } else if (page == pageShift - 1)
            return prevPage[ind]

        while (pageShift < page) {
            swapPages()
            curPageSize = inputStream.readNBytes(curPage, 0, pageSize)
            ++pageShift
            if (pageShift == 0)
                curPage.copyInto(firstPage, 0, 0, firstPageSize)
        }

        return if (ind < curPageSize)
            curPage[ind]
        else
            throw EOFExceptionHuge()
    }

    private fun swapPages() {
        val tmp = curPage
        curPage = prevPage
        prevPage = tmp
    }

    /**
     * Not implemented yet
     */
    override fun getBytes(): ByteArray? {
        return null
    }

    /**
     * Not implemented yet
     */
    override fun getBytes(offset: Int, len: Int): ByteArray? {
        return null
    }

    /**
     * write the segment (denoted by its offset and length) into an output file stream
     */
    @Throws(IOException::class)
    override fun writeToFileOutputStream(ost: FileOutputStream, os: Long, len: Long) {
        TODO()
    }

    companion object {
        private const val pageSizePower = 30
        private const val pageSize = (1 shl pageSizePower)
        private const val pageSizeLowerBits = (pageSize - 1).toLong()
        private const val firstPageSize = pageSize
    }
}

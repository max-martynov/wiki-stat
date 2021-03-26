package ru.senin.kotlin.wiki

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.junit.jupiter.api.*
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Paths
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.time.measureTime

class TestRandom {
    companion object {
        private const val TEST_DATA_PATH = "src/test/resources/testData"
        private const val TEMPORARY_DIRECTORY = "temp_test_data"
        private const val BZIP2_SUFFIX = ".bz2"

        /*@BeforeAll
        @JvmStatic
        fun createArchives() {
            val testRoot = File(TEMPORARY_DIRECTORY)
            if (!testRoot.exists()){
                testRoot.mkdirs()
            }
            File(TEST_DATA_PATH).listFiles().orEmpty().filter{ it.extension == "xml" }.forEach {
                createTemporaryBzip2File(it)
            }
            // create invalid Bzip2 file
            File("invalid".toBzip2Inputs()).writeText("Превед, Медвед!")
        }*/

        @AfterAll
        @JvmStatic
        fun cleanUp() {
            File(TEMPORARY_DIRECTORY).deleteRecursively()
        }

        private fun String.toBzip2Inputs(): String = this.split(',').joinToString(",") {
            Paths.get(TEMPORARY_DIRECTORY).resolve(it + BZIP2_SUFFIX).toString()
        }

        private fun createTemporaryBzip2File(file: File) {
            val input = file.inputStream()
            input.use {
                val output = BZip2CompressorOutputStream(FileOutputStream(file.name.toBzip2Inputs()))
                output.use {
                    input.copyTo(output)
                }
            }
        }
    }

    @Test
    fun `real XML with random optimizations`() {
        testCorrectness(threads = 4)
    }

    @Test
    fun `test effectiveness`() {
        val inputFileName = "/Users/maksimmartynov/Desktop/work/Programming/SPbSU/OOP/wiki-stat-cyber-bullies/src/test/resources/myTestData/ruwiki-20210301-pages-meta-current4.xml.bz2"
        val outputFileName = "/Users/maksimmartynov/Desktop/work/Programming/SPbSU/OOP/wiki-stat-cyber-bullies/src/test/resources/myTestData/stuff.txt"
        val durationDeterminant = measureTime {
            repeat(3) {
                main(arrayOf(
                        "--threads", (it + 3).toString(),
                        "--inputs", inputFileName,
                        "--output", outputFileName,
                        "--optimizations", "false"
                ))
            }
        }
        val durationRandom = measureTime {
            repeat(3) {
                main(arrayOf(
                        "--threads", (it + 3).toString(),
                        "--inputs", inputFileName,
                        "--output", outputFileName,
                        "--optimizations", "true"
                ))
            }
        }
        println("Determinant time: ${durationDeterminant}\nRandom time: ${durationRandom}\nDifference: ${durationDeterminant - durationRandom}")
    }

    private fun testCorrectness(threads: Int) {
        val outputFileName = "/Users/maksimmartynov/Desktop/work/Programming/SPbSU/OOP/wiki-stat-cyber-bullies/src/test/resources/myTestData/ruwiki-20210301-pages-meta-current4.real.txt"
        val args = arrayOf(
                "--threads", threads.toString(),
                "--inputs", "/Users/maksimmartynov/Desktop/work/Programming/SPbSU/OOP/wiki-stat-cyber-bullies/src/test/resources/myTestData/ruwiki-20210301-pages-meta-current4.xml.bz2",
                "--output", outputFileName,
                "--optimizations", "true"
        )
        main(args)
        val expectedFileName = "/Users/maksimmartynov/Desktop/work/Programming/SPbSU/OOP/wiki-stat-cyber-bullies/src/test/resources/myTestData/ruwiki-20210301-pages-meta-current4.expected.txt"
        assertFilesHaveSameContent(expectedFileName, outputFileName)
    }

    private fun assertFilesHaveSameContent(expectedFileName: String, actualFileName: String, message: String? = null) {
        val actual = Paths.get(TEMPORARY_DIRECTORY).resolve(actualFileName).toFile().readText()
        val expected = Paths.get(TEST_DATA_PATH).resolve(expectedFileName).toFile().readText()
        assertEquals(expected, actual, message)
    }

    private fun String.relativeToTemporaryDir(): String = Paths.get(TEMPORARY_DIRECTORY).resolve(this).toString()

}


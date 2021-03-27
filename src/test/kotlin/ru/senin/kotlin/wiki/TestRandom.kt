package ru.senin.kotlin.wiki

import org.junit.jupiter.api.*
import java.io.File
import kotlin.test.assertEquals
import kotlin.time.measureTime

class TestRandom {
    companion object {
        private const val TEMPORARY_DIRECTORY = "src/test/resources/myTestData/temporary"
        private var num = 0

        fun generateOutputFileName(): String = "${TEMPORARY_DIRECTORY}/output${num++}.txt"

        @BeforeAll
        @JvmStatic
        fun createTemporary() {
            val testRoot = File(TEMPORARY_DIRECTORY)
            if (!testRoot.exists()){
                testRoot.mkdirs()
            }
        }

        @AfterAll
        @JvmStatic
        fun cleanUp() {
            File(TEMPORARY_DIRECTORY).deleteRecursively()
        }

        fun toBzip2(s: String): String {
            val prefix = "src/test/resources/myTestData/"
            val suffix = ".xml.bz2"
            return prefix + s + suffix
        }

    }

    private val files = listOf(
            "ruwiki-20210301-pages-meta-current4", //small
            "ruwiki-20210301-pages-meta-current1", //medium
            "ruwiki-20210301-pages-meta-current2", //big
            "ruwiki-20210301-pages-meta-current6" //huge
    )

    @Test
    fun `test random optimizations`() {
        testCorrectnessAndEffectiveness(inputFileName = toBzip2(files[0]) + "," + toBzip2(files[1]))
    }

    private fun testCorrectnessAndEffectiveness(inputFileName: String) {
        val expected = generateOutputFileName()
        val actual = generateOutputFileName()

        val durationDeterministic = measureTime {
            repeat(3) {
                main(arrayOf(
                        "--threads", (it + 1).toString(),
                        "--inputs", inputFileName,
                        "--output", expected,
                        "--optimizations", "false"
                ))
            }
        }
        val durationProbabilistic = measureTime {
            repeat(3) {
                main(arrayOf(
                        "--threads", (it + 1).toString(),
                        "--inputs", inputFileName,
                        "--output", actual,
                        "--optimizations", "true"
                ))
            }
        }
        assertFilesHaveSameContent(expected, actual)
        println("Deterministic algo time: ${durationDeterministic}\n" +
                "Probabilistic algo time: ${durationProbabilistic}\n" +
                "Profit: ${((durationDeterministic - durationProbabilistic) / durationDeterministic) * 100}%")
    }

    private fun assertFilesHaveSameContent(expectedFileName: String, actualFileName: String, message: String? = null) {
        val actual = File(actualFileName).readText()
        val expected = File(expectedFileName).readText()
        assertEquals(expected, actual, message)
    }
}


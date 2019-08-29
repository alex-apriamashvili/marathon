package com.malinskiy.marathon.ios

import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.execution.TestParser
import com.malinskiy.marathon.ios.xctestrun.Xctestrun
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.test.Test
import java.io.File

class IOSTestParser : TestParser {
    private val swiftTestClassRegex = """class ([^:\s]+)\s*:\s*\w*TestCase""".toRegex()
    private val swiftTestMethodRegex = """^.*func\s+(test[^(\s]*)\s*\(.*$""".toRegex()

    private val logger = MarathonLogging.logger(IOSTestParser::class.java.simpleName)

    private fun isCompileSource(file: File): Boolean = !file.isDirectory

    private fun listTestMethods(compileSource: File): Sequence<Test> {
        var testCaseName: String = ""
        return compileSource.readLines().mapNotNull {
            testCaseName = it.firstMatchOrNull(swiftTestClassRegex) ?: testCaseName
            val methodName = it.firstMatchOrNull(swiftTestMethodRegex) ?: ""
            if (methodName.isNotEmpty() && testCaseName.isNotEmpty()) {
                Test("TransportUITests", testCaseName, methodName, emptyList())
            } else {
                null
            }
        }.asSequence()
    }

    /**
     *  Looks up test methods running a text search in swift files. Considers classes that explicitly inherit
     *  from `XCTestCase` and method names starting with `test`. Scans all swift files found under `sourceRoot`
     *  specified in Marathonfile. When not specified, starts in working directory. Result excludes any tests
     *  marked as skipped in `xctestrun` file.
     */
    override fun extract(configuration: Configuration): List<Test> {
        val vendorConfiguration = configuration.vendorConfiguration as? IOSConfiguration
                ?: throw IllegalStateException("Expected IOS configuration")

        if (!vendorConfiguration.sourceRoot.isDirectory) {
            throw IllegalArgumentException("Expected a directory at $vendorConfiguration.sourceRoot")
        }

        val xctestrun = Xctestrun(vendorConfiguration.xctestrunPath)
        val targetName = xctestrun.targetName

        val compileSources = vendorConfiguration
                .sourceRoot
                .listFiles("swift")
                .filter(::isCompileSource)
                .filter(swiftTestClassRegex)

        val testList = compileSources.flatMap(::listTestMethods)
        val filteredTests = testList.filter { !xctestrun.isSkipped(it) }.toList()

        logger.trace { filteredTests.map { "${it.clazz}.${it.method}" }.joinToString() }
        logger.info { "Found ${filteredTests.size} tests in ${compileSources.count()} files"}

        return filteredTests
    }
}

private fun Sequence<File>.filter(contentsRegex: Regex): Sequence<File> {
    return filter { it.contains(contentsRegex) }
}

private fun File.listFiles(extension: String): Sequence<File> {
    return walkTopDown().filter { it.extension == extension }
}

private val MatchResult.firstGroup: String?
    get() { return groupValues.get(1) }

private fun String.firstMatchOrNull(regex: Regex): String? {
    return regex.find(this)?.firstGroup
}

private fun File.contains(contentsRegex: Regex): Boolean {
    return inputStream().bufferedReader().lineSequence().any { it.contains(contentsRegex) }
}

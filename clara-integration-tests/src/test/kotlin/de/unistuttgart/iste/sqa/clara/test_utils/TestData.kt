package de.unistuttgart.iste.sqa.clara.test_utils

object TestData {

    val names by lazy {
        val stream = this.javaClass.classLoader.getResourceAsStream("names.txt") ?: throw Exception("Cannot load names!")
        return@lazy stream.bufferedReader().use { it.readLines() }
    }
}

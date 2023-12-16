package de.unistuttgart.iste.sqa.clara

import java.util.*

object AppInfo {
    private val buildInformation = Properties()
    private val banner: String

    init {
        buildInformation.load(this.javaClass.getResourceAsStream("/build-information.properties"))
        banner = this.javaClass.getResourceAsStream("/banner.txt")?.reader().use { it?.readText() } ?: "Unable to display the app banner!"
    }

    fun printBanner() {
        println(banner)
        println()
    }

    fun printBuildInformation() {
        println("version    : ${buildInformation.getProperty("version")}")
        println("build time : ${buildInformation.getProperty("build-time")}")
        println("git branch : ${buildInformation.getProperty("git-branch")}")
        println("git commit : ${buildInformation.getProperty("git-commit")}")
        println()
    }
}

package de.unistuttgart.iste.sqa.clara

import de.unistuttgart.iste.sqa.clara.config.AppConfig

fun main() {
    val config = AppConfig.loadFrom("/config.yml")

    val app = App(config)

    app.run()
}

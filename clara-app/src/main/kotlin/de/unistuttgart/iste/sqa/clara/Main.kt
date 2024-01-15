package de.unistuttgart.iste.sqa.clara

import de.unistuttgart.iste.sqa.clara.config.ClaraConfig

fun main() {
    val config = ClaraConfig.loadFrom("/config.yml")

    val app = App(config)

    app.run()
}

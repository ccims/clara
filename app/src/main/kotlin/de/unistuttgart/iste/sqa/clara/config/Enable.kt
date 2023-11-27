package de.unistuttgart.iste.sqa.clara.config

interface Enable {

    val enable: Boolean
}

fun <T : Enable> T.ifEnabled(func: (T) -> Unit) {
    if (this.enable) {
        func(this)
    }
}

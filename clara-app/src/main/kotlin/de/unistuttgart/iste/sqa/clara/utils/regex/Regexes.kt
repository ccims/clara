package de.unistuttgart.iste.sqa.clara.utils.regex

object Regexes {

    val ipAddressV4 = Regex("""^(25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})(\.(25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})){3}$""")
    val urlEndpoint = Regex("""^/([a-zA-Z0-9_-]+/?)+$""")
    val port = Regex("""^([0-9]|[1-9][0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$""")
    val hostName = Regex("""^(https?://)?([0-9A-Za-z](?:[0-9A-Za-z]|-){0,61}[0-9A-Za-z](?:\.[0-9A-Za-z](?:[0-9A-Za-z]|-){0,61}[0-9A-Za-z])*)""")
}

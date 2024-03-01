package de.unistuttgart.iste.sqa.clara.utils.regex

object Regexes {

    val ipAddressV4 = Regex("""^(25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})(\.(25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})){3}$""")
    val urlPath = Regex("""^/([a-zA-Z0-9_-]+/?)+$""")
    val port = Regex("""(?<!\d)([1-9]\d{0,4}|[1-5]\d{4}|6[0-4]\d{3}|65[0-4]\d{2}|655[0-2]\d|6553[0-5])(?!\d)""")
    val hostName = Regex("""^(https?://)?([0-9A-Za-z](?:[0-9A-Za-z]|-){0,61}[0-9A-Za-z](?:\.[0-9A-Za-z](?:[0-9A-Za-z]|-){0,61}[0-9A-Za-z])*)""")
}

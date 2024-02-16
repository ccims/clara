package de.unistuttgart.iste.sqa.clara.export.gropius.graphql

import java.time.Instant

data class Token(
    val value: String,
    val expiresAt: Instant,
) {

    fun isExpired(): Boolean {
        return Instant.now().isAfter(expiresAt)
    }
}

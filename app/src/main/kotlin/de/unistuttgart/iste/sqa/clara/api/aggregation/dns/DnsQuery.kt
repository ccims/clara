package de.unistuttgart.iste.sqa.clara.api.aggregation.dns

import de.unistuttgart.iste.sqa.clara.api.model.Domain
import de.unistuttgart.iste.sqa.clara.api.model.IpAddress

data class DnsQuery(
    val sourceIpAddress: IpAddress,
    val targetDomain: Domain,
)

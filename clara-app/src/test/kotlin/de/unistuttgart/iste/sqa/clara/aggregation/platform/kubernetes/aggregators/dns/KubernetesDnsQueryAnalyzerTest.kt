package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.dns

import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.client.KubernetesPod
import de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.client.KubernetesService
import de.unistuttgart.iste.sqa.clara.api.model.Communication
import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContainExactly

class KubernetesDnsQueryAnalyzerTest : FreeSpec({

    data class TestExample(
        val knownPods: List<KubernetesPod>,
        val knownKubernetesServices: List<KubernetesService>,
        val dnsQueries: List<DnsQuery>,
        val expected: Set<Communication>,
    )

    // TODO
    val testExamples = listOf(
        TestExample(
            knownPods = listOf(),
            knownKubernetesServices = listOf(),
            dnsQueries = listOf(),
            expected = setOf()
        ),
        TestExample(
            knownPods = listOf(),
            knownKubernetesServices = listOf(),
            dnsQueries = listOf(),
            expected = setOf()
        ),
        TestExample(
            knownPods = listOf(),
            knownKubernetesServices = listOf(),
            dnsQueries = listOf(),
            expected = setOf()
        ),
    )

    "analyze" - {

        "should parse DNS queries correctly" - {

            withData(testExamples) { (knownPods, knownServices, dnsQueries, expected) ->

                val actual = KubernetesDnsQueryAnalyzer(knownPods, knownServices).analyze(dnsQueries)

                actual shouldContainExactly expected
            }
        }
    }
})

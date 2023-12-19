package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.dns

import de.unistuttgart.iste.sqa.clara.api.model.Domain
import de.unistuttgart.iste.sqa.clara.api.model.IpAddress
import io.kotest.core.spec.style.FreeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContainExactly

class KubernetesDnsLogAnalyzerTest : FreeSpec({

    data class TestExample(val logs: String, val expected: Set<DnsQuery>)

    val testExamples = listOf(
        TestExample(
            logs = """
                [INFO] plugin/reload: Running configuration SHA512 = f869070685748660180df1b7a47d58cdafcf2f368266578c062d1151dc2c900964aecc5975e8882e6de6fdfb6460463e30ebfaad2ec8f0c3c6436f80225b3b5b
                CoreDNS-1.10.1
                linux/amd64, go1.20, 055b2c3
                [INFO] 127.0.0.1:37480 - 43907 "HINFO IN 2986383786970326134.1394570990575653605. udp 57 false 512" NXDOMAIN qr,rd,ra 57 0.027325642s                
            """.trimIndent(),
            expected = emptySet()
        ),
        TestExample(
            logs = """
                [INFO] 10.244.0.19:55105 - 45236 "A IN 10-244-0-18.default.pod.cluster.local.cluster.local. udp 69 false 512" NXDOMAIN qr,aa,rd 162 0.000080099s
                [INFO] 10.244.0.19:40667 - 53463 "A IN 10-244-0-18.default.pod.cluster.local. udp 55 false 512" NOERROR qr,aa,rd 108 0.000090999s
                [INFO] 10.244.0.19:58468 - 36100 "A IN kubernetes.default.svc.cluster.local.default.svc.cluster.local. udp 80 false 512" NXDOMAIN qr,aa,rd 173 0.000131699s
                [INFO] 10.244.0.19:35065 - 3179 "A IN kubernetes.default.svc.cluster.local.svc.cluster.local. udp 72 false 512" NXDOMAIN qr,aa,rd 165 0.0000838s
                [INFO] 10.244.0.19:33307 - 32127 "A IN kubernetes.default.svc.cluster.local.cluster.local. udp 68 false 512" NXDOMAIN qr,aa,rd 161 0.0000589s
                [INFO] 10.244.0.19:56981 - 16604 "A IN kubernetes.default.svc.cluster.local. udp 54 false 512" NOERROR qr,aa,rd 106 0.000093199s
                [INFO] 10.244.0.19:44910 - 42388 "A IN google.com.default.svc.cluster.local. udp 54 false 512" NXDOMAIN qr,aa,rd 147 0.0001194s
                [INFO] 10.244.0.19:36953 - 51945 "A IN google.com.svc.cluster.local. udp 46 false 512" NXDOMAIN qr,aa,rd 139 0.0000822s
                [INFO] 10.244.0.19:39392 - 8968 "A IN google.com.cluster.local. udp 42 false 512" NXDOMAIN qr,aa,rd 135 0.000101099s
                [INFO] 10.244.0.19:35188 - 24703 "A IN google.com. udp 28 false 512" NOERROR qr,rd,ra 54 0.004511573s
                [INFO] 10.244.0.19:44911 - 42388 "A IN google.com.default.svc.cluster.local. udp 54 false 512" NXDOMAIN qr,aa,rd 147 0.0001194s
                [INFO] 10.244.0.19:36922 - 51945 "A IN google.com.svc.cluster.local. udp 46 false 512" NXDOMAIN qr,aa,rd 139 0.0000822s
                [INFO] 10.244.0.19:39345 - 8968 "A IN google.com.cluster.local. udp 42 false 512" NXDOMAIN qr,aa,rd 135 0.000101099s
                [INFO] 10.244.0.19:35328 - 24703 "A IN google.com. udp 28 false 512" NOERROR qr,rd,ra 54 0.004511573s
            """.trimIndent(),
            expected = setOf(
                DnsQuery(IpAddress("10.244.0.19"), Domain("10-244-0-18.default.pod.cluster.local.")),
                DnsQuery(IpAddress("10.244.0.19"), Domain("kubernetes.default.svc.cluster.local.")),
                DnsQuery(IpAddress("10.244.0.19"), Domain("google.com.")),
            )
        ),
        TestExample(
            logs = """
                .:53
                [INFO] plugin/reload: Running configuration SHA512 = f869070685748660180df1b7a47d58cdafcf2f368266578c062d1151dc2c900964aecc5975e8882e6de6fdfb6460463e30ebfaad2ec8f0c3c6436f80225b3b5b
                CoreDNS-1.10.1
                linux/amd64, go1.20, 055b2c3
                [INFO] 127.0.0.1:37480 - 43907 "HINFO IN 2986383786970326134.1394570990575653605. udp 57 false 512" NXDOMAIN qr,rd,ra 57 0.027325642s
                [INFO] 10.244.0.19:41296 - 10944 "A IN 10-244-0-18.default.pod.cluster.local.default.svc.cluster.local. udp 81 false 512" NXDOMAIN qr,aa,rd 174 0.000140197s
                [INFO] 10.244.0.19:41335 - 56869 "A IN 10-244-0-18.default.pod.cluster.local.svc.cluster.local. udp 73 false 512" NXDOMAIN qr,aa,rd 166 0.000106598s
                [INFO] 10.244.0.19:55105 - 45236 "A IN 10-244-0-18.default.pod.cluster.local.cluster.local. udp 69 false 512" NXDOMAIN qr,aa,rd 162 0.000080099s
                [INFO] 10.244.0.19:40667 - 53463 "A IN 10-244-0-18.default.pod.cluster.local. udp 55 false 512" NOERROR qr,aa,rd 108 0.000090999s
                [INFO] 10.244.0.19:58468 - 36100 "A IN kubernetes.default.svc.cluster.local.default.svc.cluster.local. udp 80 false 512" NXDOMAIN qr,aa,rd 173 0.000131699s
                [INFO] 10.244.0.19:35065 - 3179 "A IN kubernetes.default.svc.cluster.local.svc.cluster.local. udp 72 false 512" NXDOMAIN qr,aa,rd 165 0.0000838s
                [INFO] 10.244.0.19:33307 - 32127 "A IN kubernetes.default.svc.cluster.local.cluster.local. udp 68 false 512" NXDOMAIN qr,aa,rd 161 0.0000589s
                [INFO] 10.244.0.19:56981 - 16604 "A IN kubernetes.default.svc.cluster.local. udp 54 false 512" NOERROR qr,aa,rd 106 0.000093199s
                [INFO] 10.244.0.19:44910 - 42388 "A IN google.com.default.svc.cluster.local. udp 54 false 512" NXDOMAIN qr,aa,rd 147 0.0001194s
                [INFO] 10.244.0.19:36953 - 51945 "A IN google.com.svc.cluster.local. udp 46 false 512" NXDOMAIN qr,aa,rd 139 0.0000822s
                [INFO] 10.244.0.19:39392 - 8968 "A IN google.com.cluster.local. udp 42 false 512" NXDOMAIN qr,aa,rd 135 0.000101099s
                [INFO] 10.244.0.19:35188 - 24703 "A IN google.com. udp 28 false 512" NOERROR qr,rd,ra 54 0.004511573s
                [INFO] 10.244.0.19:44083 - 22068 "A IN a.a.a.a.a.a.non-existent.com. udp 46 false 512" NXDOMAIN qr,rd,ra 46 0.797013679s
                [INFO] 10.244.0.19:44083 - 22068 "A IN a.a.a.a.a.a.non-existent.com. udp 46 false 512" NXDOMAIN qr,rd,ra 46 5.797061292s
                [INFO] 10.244.0.19:52828 - 8802 "A IN kubernetes.default.svc.cluster.local.default.svc.cluster.local. udp 80 false 512" NXDOMAIN qr,aa,rd 173 0.000133903s
                [INFO] 10.244.0.19:48096 - 54478 "A IN kubernetes.default.svc.cluster.local.svc.cluster.local. udp 72 false 512" NXDOMAIN qr,aa,rd 165 0.000094501s
                [INFO] 10.244.0.19:53731 - 32267 "A IN kubernetes.default.svc.cluster.local.cluster.local. udp 68 false 512" NXDOMAIN qr,aa,rd 161 0.000108602s
                [INFO] 10.244.0.19:57841 - 50626 "A IN kubernetes.default.svc.cluster.local. udp 54 false 512" NOERROR qr,aa,rd 106 0.000088102s
                [INFO] 10.244.0.19:50142 - 23574 "A IN 1-1-1-1.default.pod.cluster.local.default.svc.cluster.local. udp 77 false 512" NXDOMAIN qr,aa,rd 170 0.000135502s
                [INFO] 10.244.0.19:52534 - 38513 "A IN 1-1-1-1.default.pod.cluster.local.svc.cluster.local. udp 69 false 512" NXDOMAIN qr,aa,rd 162 0.000098902s
                [INFO] 10.244.0.19:37469 - 190 "A IN 1-1-1-1.default.pod.cluster.local.cluster.local. udp 65 false 512" NXDOMAIN qr,aa,rd 158 0.000090501s
                abcdefghijklmnopqrstuvwxyz 0123456789
                [INFO] 10.244.0.19:56861 - 34320 "A IN 1-1-1-1.default.pod.cluster.local. udp 51 false 512" NOERROR qr,aa,rd 100 0.000107401s
                [INFO] 10.244.0.19:49793 - 12632 "A IN 1-1-1-100.default.pod.cluster.local.default.svc.cluster.local. udp 79 false 512" NXDOMAIN qr,aa,rd 172 0.000117899s
                [INFO] 10.244.0.19:50420 - 4049 "A IN 1-1-1-100.default.pod.cluster.local.svc.cluster.local. udp 71 false 512" NXDOMAIN qr,aa,rd 164 0.000115999s
                [INFO] 10.244.0.19:46965 - 60000 "A IN 1-1-1-100.default.pod.cluster.local.cluster.local. udp 67 false 512" NXDOMAIN qr,aa,rd 160 0.000076399s
                [INFO] 10.244.0.19:37649 - 43088 "A IN 1-1-1-100.default.pod.cluster.local. udp 53 false 512" NOERROR qr,aa,rd 104 0.000086399s
                [INFO] 10.244.0.19:47215 - 43864 "A IN non-existent-service.namespace.svc.cluster.local.default.svc.cluster.local. udp 92 false 512" NXDOMAIN qr,aa,rd 185 0.000121401s
                [INFO] 10.244.0.19:56197 - 14661 "A IN non-existent-service.namespace.svc.cluster.local.svc.cluster.local. udp 84 false 512" NXDOMAIN qr,aa,rd 177 0.0000929s
                [INFO] 10.244.0.19:45806 - 9463 "A IN non-existent-service.namespace.svc.cluster.local.cluster.local. udp 80 false 512" NXDOMAIN qr,aa,rd 173 0.0001237s
                [INFO] 10.244.0.19:58494 - 10384 "A IN non-existent-service.namespace.svc.cluster.local. udp 66 false 512" NXDOMAIN qr,aa,rd 159 0.000107301s
                [INFO] 10.244.0.19:47854 - 3965 "A IN kubernetes.default.svc.cluster.local.default.svc.cluster.local. udp 80 false 512" NXDOMAIN qr,aa,rd 173 0.0001347s
                [INFO] 10.244.0.19:33688 - 20187 "A IN kubernetes.default.svc.cluster.local.svc.cluster.local. udp 72 false 512" NXDOMAIN qr,aa,rd 165 0.000111301s
                [INFO] 10.244.0.19:51793 - 49441 "A IN kubernetes.default.svc.cluster.local.cluster.local. udp 68 false 512" NXDOMAIN qr,aa,rd 161 0.000095201s
                [INFO] 10.244.0.19:50096 - 17198 "A IN kubernetes.default.svc.cluster.local. udp 54 false 512" NOERROR qr,aa,rd 106 0.000119501s
                [INFO] 10.244.0.22:12456 - 17198 "A IN kubernetes.default.svc.cluster.local. udp 54 false 512" NOERROR
            """.trimIndent(),
            expected = setOf(
                DnsQuery(sourceIpAddress = IpAddress("10.244.0.19"), targetDomain = Domain("google.com.")),
                DnsQuery(sourceIpAddress = IpAddress("10.244.0.19"), targetDomain = Domain("1-1-1-1.default.pod.cluster.local.")),
                DnsQuery(sourceIpAddress = IpAddress("10.244.0.19"), targetDomain = Domain("1-1-1-100.default.pod.cluster.local.")),
                DnsQuery(sourceIpAddress = IpAddress("10.244.0.19"), targetDomain = Domain("10-244-0-18.default.pod.cluster.local.")),
                DnsQuery(sourceIpAddress = IpAddress("10.244.0.19"), targetDomain = Domain("kubernetes.default.svc.cluster.local.")),
                DnsQuery(sourceIpAddress = IpAddress("10.244.0.22"), targetDomain = Domain("kubernetes.default.svc.cluster.local.")),
            )
        ),
    )

    "parseLogs" - {

        "should parse Kubernetes DNS logs correctly" - {

            withData(testExamples) { (logs, expected) ->

                val actual = KubernetesDnsLogAnalyzer.parseLogs(logs)

                actual shouldContainExactly expected
            }
        }
    }
})

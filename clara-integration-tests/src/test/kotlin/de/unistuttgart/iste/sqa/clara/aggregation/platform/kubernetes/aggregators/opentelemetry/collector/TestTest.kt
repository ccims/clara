package de.unistuttgart.iste.sqa.clara.aggregation.platform.kubernetes.aggregators.opentelemetry.collector

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class TestTest : FreeSpec({

    "should pass" - {
        1 shouldBe 1
    }

    "should fail" - {
        1 shouldBe 199
    }
})

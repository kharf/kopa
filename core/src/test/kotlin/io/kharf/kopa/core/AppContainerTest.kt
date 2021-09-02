package io.kharf.kopa.core

import failgood.describe
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.hasSize

@ExperimentalSerializationApi
@Testable
class AppContainerTest {
    val context = describe(AppContainer::class) {
        val subject = AppContainer
        describe(AppContainer::init.toString()) {
            it("should create a simple project") {
                val path = "build"
                val template = subject.init(Path(path))
                expectThat(template).hasSize(1)
            }
        }
    }
}

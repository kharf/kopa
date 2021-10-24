package io.kharf.kopa.core

import failgood.describe
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@ExperimentalSerializationApi
@Testable
class KotlinJvmBuilderTest {
    val context = describe(KotlinJvmBuilder::class) {
        val subject = KotlinJvmBuilder
        describe(KotlinJvmBuilder::build.toString()) {
            it("should build a simple container") {
                val path = Path("build/builder-testsample")
                AppContainer(subject).init(path)
                val code = subject.build(path)
                expectThat(code).isEqualTo(ExitCode.OK)
            }

            it("should return an erroneous ExitCode if a container could not be built") {
                val path = Path("build/non-existing-builder-testsample")
                val code = subject.build(path)
                expectThat(code).isEqualTo(ExitCode.COMPILATION_ERROR)
            }
        }
    }
}

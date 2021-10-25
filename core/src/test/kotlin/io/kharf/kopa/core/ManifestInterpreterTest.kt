package io.kharf.kopa.core

import com.akuleshov7.ktoml.KtomlConf
import com.akuleshov7.ktoml.parsers.TomlParser
import com.akuleshov7.ktoml.parsers.node.TomlKeyValueSimple
import com.akuleshov7.ktoml.parsers.node.TomlTable
import failgood.describe
import kotlinx.serialization.ExperimentalSerializationApi
import mu.KotlinLogging
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.lang.RuntimeException

@ExperimentalSerializationApi
@Testable
class ManifestInterpreterTest {
    val context = describe(ManifestInterpreter::class) {
        val subject = object : ManifestInterpreter<String> {
            private val logger = KotlinLogging.logger { }
            override fun interpret(manifest: String): Interpretation {
                val toml = TomlParser(KtomlConf()).parseString(manifest)
                val dependencies: TomlTable =
                    toml.children.find { node -> node.name == "dependencies" && node is TomlTable } as TomlTable?
                        ?: throw RuntimeException()
                logger.info { dependencies.children }
                val filteredDependencies = dependencies.children.filterIsInstance<TomlKeyValueSimple>()
                val builder = StringBuilder("build/classpath/")
                filteredDependencies.forEach { dependency ->
                    builder.append("${dependency.key.content}-${dependency.value.content}.jar")
                }
                val classpath = builder.toString()
                return Interpretation(classpath)
            }
        }
        describe(ManifestInterpreter<String>::interpret.toString()) {
            it("interpret") {
                val interpretation = subject.interpret(
                    """
                       [container]
                       name = "builder-testsample"
                       version = "0.1.0"

                       [dependencies]
                       kotlin-stdlib = "1.5.32"
                    """.trimIndent()
                )
                expectThat(interpretation) {
                    get { classpath }.isEqualTo("build/classpath/kotlin-stdlib-1.5.32.jar")
                }
            }
        }
    }
}

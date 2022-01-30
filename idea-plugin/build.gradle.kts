plugins {
    id("io.kharf.kopa.kotlin-common-conventions")
    id("org.jetbrains.intellij") version "1.3.1"
    kotlin("jvm")
}

dependencies {
    implementation("com.akuleshov7:ktoml-core:0.2.9")
    implementation("com.akuleshov7:ktoml-file:0.2.9")
    implementation("com.squareup.okio:okio:3.0.0")
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version.set("2021.3.2")
}
tasks {
    patchPluginXml {
        changeNotes.set(
            """
            Add change notes here.<br>
            <em>most HTML tags may be used</em>        
            """.trimIndent()
        )
    }
}

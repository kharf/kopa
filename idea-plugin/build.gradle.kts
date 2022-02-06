plugins {
    id("io.kharf.kopa.kotlin-common-conventions")
    id("org.jetbrains.intellij") version "1.3.1"
    kotlin("jvm")
}

dependencies {
    implementation(project(":packages"))
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

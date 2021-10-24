import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Support convention plugins written in Kotlin. Convention plugins are build scripts in 'src/main' that automatically become available as plugins in the main build.
    `kotlin-dsl`
}

repositories {
    // Use the plugin portal to apply community plugins in convention plugins.
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:10.2.0")
    implementation("com.github.ben-manes:gradle-versions-plugin:0.39.0")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "16"
        targetCompatibility = "16"
    }
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "16"
            targetCompatibility = "16"
        }
    }
}

plugins {
    id("io.kharf.kopa.kotlin-common-conventions")
    kotlin("jvm")
    kotlin("plugin.serialization") version Versions.kotlin
}

dependencies {
    implementation(project(":packages"))
    implementation("org.jetbrains.kotlin:kotlin-compiler")
    implementation("com.squareup.okio:okio:${Versions.okio}")
    testImplementation("com.squareup.okio:okio-fakefilesystem:${Versions.okio}")
    testImplementation(project(":test-utilities"))
}

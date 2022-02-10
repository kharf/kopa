plugins {
    id("io.kharf.kopa.kotlin-common-conventions")
    kotlin("jvm")
    kotlin("plugin.serialization") version Versions.kotlin
}

dependencies {
    api("com.akuleshov7:ktoml-core:${Versions.ktoml}")
    implementation("com.akuleshov7:ktoml-file:${Versions.ktoml}")
    implementation("io.github.pdvrieze.xmlutil:core-jvm:${Versions.xmlutil}")
    implementation("io.github.pdvrieze.xmlutil:serialization-jvm:${Versions.xmlutil}")
    api("com.squareup.okio:okio:${Versions.okio}")
    implementation("io.ktor:ktor-client-core:${Versions.ktor}")
    implementation("io.ktor:ktor-client-cio:${Versions.ktor}")
    testImplementation("com.squareup.okio:okio-fakefilesystem:${Versions.okio}")
    testImplementation(project(":test-utilities"))
}

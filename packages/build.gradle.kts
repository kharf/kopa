plugins {
    id("io.kharf.kopa.kotlin-common-conventions")
    kotlin("jvm")
    kotlin("plugin.serialization") version Versions.kotlin
}

dependencies {
    api("com.akuleshov7:ktoml-core:${Versions.ktoml}")
    implementation("com.akuleshov7:ktoml-file:${Versions.ktoml}")
    implementation("org.apache.maven:maven-model:${Versions.mavenModel}")
    api("com.squareup.okio:okio:${Versions.okio}")
    implementation("io.ktor:ktor-client-core:${Versions.ktor}")
    implementation("io.ktor:ktor-client-cio:${Versions.ktor}")
    testImplementation("com.squareup.okio:okio-fakefilesystem:${Versions.okio}")
    testImplementation(project(":test-utilities"))
}

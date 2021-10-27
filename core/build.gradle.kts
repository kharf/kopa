plugins {
    id("io.kharf.kopa.kotlin-common-conventions")
    kotlin("jvm")
    kotlin("plugin.serialization") version Versions.kotlin
}

dependencies {
    implementation(project(":utilities"))
    implementation("com.akuleshov7:ktoml-core:${Versions.ktoml}")
    implementation("com.akuleshov7:ktoml-file:${Versions.ktoml}")
    implementation("org.jetbrains.kotlin:kotlin-compiler")
    implementation("ch.qos.logback:logback-classic:${Versions.logback}")
    implementation("com.squareup.okio:okio:${Versions.okio}")
    implementation("org.apache.maven.resolver:maven-resolver-api:${Versions.mavenResolverApi}")
    implementation("io.ktor:ktor-client-core:${Versions.ktor}")
    implementation("io.ktor:ktor-client-cio:${Versions.ktor}")
    testImplementation("com.squareup.okio:okio-fakefilesystem:${Versions.okio}")
}

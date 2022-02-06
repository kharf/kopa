plugins {
    id("io.kharf.kopa.kotlin-application-conventions")
}

dependencies {
    implementation(project(":compiler"))
    implementation(project(":packages"))
    implementation("com.github.ajalt.clikt:clikt:${Versions.clikt}")
    implementation("com.squareup.okio:okio:${Versions.okio}")
}

application {
    // Define the main class for the application.
    mainClass.set("io.kharf.kopa.cli.AppKt")
}

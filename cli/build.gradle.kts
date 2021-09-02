plugins {
    id("io.kharf.koship.kotlin-application-conventions")
}

dependencies {
    implementation(project(":core"))
    implementation("com.github.ajalt.clikt:clikt:$cliktVersion")
}

application {
    // Define the main class for the application.
    mainClass.set("io.kharf.koship.app.AppKt")
}

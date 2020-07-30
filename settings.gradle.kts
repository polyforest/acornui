pluginManagement {
    repositories {
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
    }
}

include("acornui-gradle-plugins", "acornui-core")

rootProject.name = "acornui"


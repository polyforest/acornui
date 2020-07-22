pluginManagement {
    val kotlinVersion: String by settings
    repositories {
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
    }
    resolutionStrategy {
        eachPlugin {
            when(requested.id.namespace) {
                "org.jetbrains.kotlin.plugin",
                "org.jetbrains.kotlin" ->
                    useVersion(kotlinVersion)
            }
        }
    }
}

include("acornui-gradle-plugins", "acornui-core")

rootProject.name = "acornui"


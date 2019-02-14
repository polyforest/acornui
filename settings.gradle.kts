pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.jetbrains.kotlin.multiplatform") {
                useVersion(KOTLIN_VERSION)
            }
        }
    }
}

val settings = this as ExtensionAware
val KOTLIN_VERSION: String by settings.extra
rootProject.name = "acornui"

// Uncomment below and adapt, adding modules as they are created.  Modules take on the name of their root directory in gradle.
include("acornui-core", "acornui-game", "acornui-spine", "acornui-utils", "backends:acornui-lwjgl-backend", "backends:acornui-webgl-backend", "tools:acornui-build-tasks", "tools:acornui-texture-packer")
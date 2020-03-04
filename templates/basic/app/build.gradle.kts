plugins {
	id("com.acornui.app")
}

val acornVersion: String by project
kotlin {
	sourceSets {
		commonMain {
			dependencies {

				// Depend on the assets to be used by BasicUiSkin
				runtimeOnly("com.acornui.skins:basic:$acornVersion")
			}
		}
	}
}

tasks.runJvm {
	// The main entry point for the runJvm task.
	// main.kt has package-level functions that will be compiled to a class named "MainKt"
	// see https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html#package-level-functions
	main = "MainKt"
}
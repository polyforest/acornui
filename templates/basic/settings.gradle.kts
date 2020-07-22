rootProject.name = "example"

pluginManagement {
	val acornVersion: String by settings
	val kotlinVersion: String by settings
	repositories {
		maven("https://dl.bintray.com/kotlin/kotlin-eap")
		maven("https://oss.sonatype.org/content/repositories/snapshots")
		gradlePluginPortal()
		mavenCentral()
		jcenter()
		mavenLocal()
	}
	resolutionStrategy {
		eachPlugin {
			when(requested.id.namespace) {
				"com.acornui" -> useVersion(acornVersion)
				"org.jetbrains.kotlin.plugin",
				"org.jetbrains.kotlin" ->
					useVersion(kotlinVersion)
			}
		}
	}
}

// Add modules as they are created.  By default, subprojects take on the name of their root directory in gradle.
include("app")
rootProject.name = "example"

pluginManagement {
	val acornVersion: String by settings
	repositories {
		gradlePluginPortal()
		mavenCentral()
		jcenter()
		maven("https://oss.sonatype.org/content/repositories/snapshots")
		mavenLocal()
	}
	resolutionStrategy {
		eachPlugin {
			when(requested.id.namespace) {
				"com.acornui" -> useVersion(acornVersion)
			}
		}
	}
}

// Add modules as they are created.  By default, subprojects take on the name of their root directory in gradle.
include("app")
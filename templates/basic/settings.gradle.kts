rootProject.name = "basic-acorn-project"

pluginManagement {
	val acornVersion: String by settings
	buildscript {
		repositories {
			if (acornVersion.endsWith("-SNAPSHOT")) {
				maven("https://oss.sonatype.org/content/repositories/snapshots")
				mavenLocal()
			}
			mavenCentral()
			jcenter()
			maven("https://dl.bintray.com/kotlin/kotlin-eap/")
		}
		dependencies {
			classpath("com.acornui:gradle-app-plugins:$acornVersion")
		}
	}
}
apply(plugin = "com.acornui.settings")

// Add sub-projects here:
include("app")
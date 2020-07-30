import org.apache.tools.ant.filters.ReplaceTokens
import java.util.*
import org.gradle.kotlin.dsl.*

plugins {
	kotlin("js").version(Config.KOTLIN_VERSION) apply false
	kotlin("plugin.serialization").version(Config.KOTLIN_VERSION) apply false
}

allprojects {
	repositories {
        maven("https://kotlin.bintray.com/kotlinx")
		mavenCentral()
		jcenter()
	}
}

val buildTemplatesTask = tasks.register<Sync>("buildTemplates") {
	exclude("**/build")
	exclude("**/.idea")
	exclude("**/.gradle")
	into(buildDir.resolve("templates"))
	from("templates") {
		filesMatching("**/*.properties") {
			filter(mapOf("tokens" to mapOf("acornVersion" to version)), ReplaceTokens::class.java)
		}
		filesMatching("**/*.txt") {
			filter(
                mapOf("tokens" to mapOf("acornVersion" to version, "date" to Date().toString())),
                ReplaceTokens::class.java
            )
		}
	}
}

val archiveBasicTemplate = tasks.register<Zip>("archiveBasicTemplate") {
	exclude("**/build")
	exclude("**/.idea")
	exclude("**/.gradle")
	group = "publishing"
	dependsOn(buildTemplatesTask)
	archiveBaseName.set("acornUi")
	from(buildDir.resolve("templates/basic"))
}

tasks.register("publishToMavenLocal") {
	group = "publishing"
}

tasks.register("publish") {
	group = "publishing"
}

tasks.named("publishToMavenLocal") {
	dependsOn(archiveBasicTemplate)
}

tasks.named("publish") {
	dependsOn(archiveBasicTemplate)
}
import org.apache.tools.ant.filters.ReplaceTokens
import java.util.*
import org.gradle.kotlin.dsl.*

plugins {
	base
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

tasks.register("publishToMavenLocal") {
	group = "publishing"
}

val archiveBasicTemplateTask = tasks.register<Zip>("archiveBasicTemplate") {
	exclude("**/build")
	exclude("**/.idea")
	exclude("**/.gradle")
	group = "publishing"
	dependsOn(buildTemplatesTask)
	archiveBaseName.set("acornUi")
	from(buildDir.resolve("templates/basic"))
}

val archiveTemplatesTask = tasks.register("archiveTemplates") {
	dependsOn(archiveBasicTemplateTask)
}

val assembleTask = tasks.named("assemble") {
	dependsOn(archiveTemplatesTask)
}

tasks.register("publish") {
	group = "publishing"
}

val testBasicTemplateTask = tasks.register<GradleBuild>("testBasicTemplate") {
	dependsOn(buildTemplatesTask)
	subprojects.forEach {
		dependsOn(it.tasks.named("publishToMavenLocal"))
	}
	group = "verification"
	dir = buildDir.resolve("templates/basic")
	tasks = listOf("build")
	startParameter.projectProperties["acornVersion"] = version.toString()
}

val testTemplatesTask = tasks.register("testTemplates") {
	dependsOn(testBasicTemplateTask)
	group = "verification"
}

tasks.named("check") {
	dependsOn(testTemplatesTask)
}
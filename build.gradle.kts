import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.apache.tools.ant.filters.ReplaceTokens
import java.util.Date

plugins {
    kotlin("js") apply false
    kotlin("plugin.serialization") apply false
}

allprojects {
    repositories {
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        mavenCentral()
        jcenter()
    }
}

listOf("acornui-core").map { project(it) }.forEach {
    with(it) {
        apply(from = "$rootDir/mavenPublish.gradle.kts")
        pluginManager.apply("org.jetbrains.kotlin.js")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

        val kotlinSourcesJar by tasks.named("kotlinSourcesJar")
        extensions.getByType<PublishingExtension>().publications {
            create<MavenPublication>("default") {
                from(components["kotlin"])
                artifact(kotlinSourcesJar)
            }
        }

        extensions.configure<KotlinJsProjectExtension> {
            val kotlinVersion: String by project
            val kotlinSerializationVersion: String by project
            val kotlinCoroutinesVersion: String by project
            val kotlinLanguageVersion: String by project

            target {
                compilations.configureEach {
                    kotlinOptions {
                        languageVersion = kotlinLanguageVersion
                    }
                }
                browser {
                    testTask {
                        useMocha {
                            // For async tests use runMainTest and runHeadlessTest which use their own timeout.
                            timeout = "30s"
                        }
                    }
                }
                nodejs {
                    testTask {
                        useMocha {
                            // For async tests use runMainTest and runHeadlessTest which use their own timeout.
                            timeout = "30s"
                        }
                    }
                }
            }

            sourceSets {
                all {
                    languageSettings.apply {
                        enableLanguageFeature("InlineClasses")
                        useExperimentalAnnotation("kotlin.Experimental")
                        useExperimentalAnnotation("kotlin.time.ExperimentalTime")
                        useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
                        useExperimentalAnnotation("kotlinx.serialization.ImplicitReflectionSerializer")
                        useExperimentalAnnotation("kotlinx.coroutines.InternalCoroutinesApi")
                    }
                }

                val main by getting {
                    dependencies {
                        implementation(npm("promise-polyfill", version = "8.1.3")) // For IE11
                        implementation(npm("resize-observer-polyfill", version = "1.5.1")) // For IE11 and Edge
                        implementation(kotlin("stdlib-js", version = kotlinVersion))

                        api("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.2")
                        api("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$kotlinSerializationVersion")
                        api("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$kotlinSerializationVersion")
                        api("org.jetbrains.kotlinx:kotlinx-coroutines-core-common:$kotlinCoroutinesVersion")
                        api("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$kotlinCoroutinesVersion")
                    }
                }

                val test by getting {
                    dependencies {
                        implementation(kotlin("test", version = kotlinVersion))
                        implementation(kotlin("test-js", version = kotlinVersion))
                        implementation(npm("jsdom", version = "16.2.2")) // simulate window/document
                    }
                }
            }
        }
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
            filter(mapOf("tokens" to mapOf("acornVersion" to version, "date" to Date().toString())), ReplaceTokens::class.java)
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

tasks.register("publishToMavenLocal")
tasks.register("publish")

tasks.named("publishToMavenLocal") {
    dependsOn(archiveBasicTemplate)
}

tasks.named("publish") {
    dependsOn(archiveBasicTemplate)
}
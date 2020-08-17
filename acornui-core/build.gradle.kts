/*
 * Copyright 2020 Poly Forest, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
	`maven-publish`
	kotlin("js")
	kotlin("plugin.serialization")
}

apply(from = "$rootDir/mavenPublish.gradle.kts")

dependencies {
	// IE and Edge no longer supported
//	implementation(npm("promise-polyfill", version = "8.1.3")) // For IE11
//	implementation(npm("resize-observer-polyfill", version = "1.5.1")) // For IE11 and Edge
	api(npm("focus-visible", version = "5.1.0"))
	api(kotlin("stdlib", version = Config.KOTLIN_VERSION))
	api(kotlin("stdlib-js", version = Config.KOTLIN_VERSION))
	api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Config.KOTLIN_COROUTINES_VERSION}")
	api("org.jetbrains.kotlinx:kotlinx-serialization-core:${Config.KOTLIN_SERIALIZATION_VERSION}")
	api("org.jetbrains.kotlinx:kotlinx-collections-immutable-js:0.3.2")

	testImplementation(kotlin("test-js"))
	testImplementation(devNpm("jsdom", version = "16.2.2")) // simulate window/document
}

kotlin {
	js {

		browser {
			testTask {
				//				useMocha {
				//					// For async tests use runMainTest and runHeadlessTest which use their own timeout.
				//					timeout = "30s"
				//				}
				useKarma {
					useChromeHeadless()
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

		sourceSets {
			all {
				languageSettings.apply {
					languageVersion = Config.KOTLIN_LANGUAGE_VERSION
					apiVersion = Config.KOTLIN_LANGUAGE_VERSION
					enableLanguageFeature("InlineClasses")
					useExperimentalAnnotation("kotlin.Experimental")
					useExperimentalAnnotation("kotlin.time.ExperimentalTime")
					useExperimentalAnnotation("kotlin.contracts.ExperimentalContracts")
					useExperimentalAnnotation("kotlinx.coroutines.InternalCoroutinesApi")
				}
			}
		}
	}
}

val kotlinSourcesJar by tasks.named("kotlinSourcesJar")
publishing.publications {
	create<MavenPublication>("default") {
		from(components["kotlin"])
		artifact(kotlinSourcesJar)
	}
}

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

@file:Suppress("UnstableApiUsage")

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


import groovy.util.Node
import org.gradle.api.internal.artifacts.repositories.resolver.ExternalResourceResolver
import de.marcphilipp.gradle.nexus.NexusPublishPlugin
import de.marcphilipp.gradle.nexus.NexusPublishExtension
import java.time.Duration

buildscript {
	repositories {
		maven("https://plugins.gradle.org/m2/")
	}
	dependencies {
		classpath("de.marcphilipp.gradle:nexus-publish-plugin:0.3.0")
	}
}

// Thanks: https://github.com/h0tk3y/k-new-mpp-samples/blob/master/publish-to-maven-central/build.gradle.kts

// Configure signing and publishing extensions.

val Project.isSnapshot: Boolean
	get() = version.toString().endsWith("-SNAPSHOT")

val signingKeyId: String? by project
val signingKey: String? by project
val signingPassword: String? by project
val hasSigningProps = signingKeyId != null && signingKey != null && signingPassword != null

val ossrhUsername: String? by project
val ossrhPassword: String? by project

if (ossrhUsername != null && ossrhPassword != null) {
	if (!isSnapshot) require(hasSigningProps) { "Staged production releases must have signing credentials set. (signingKeyId, signingKey, and signingPassword)" }
	apply<NexusPublishPlugin>()
	the<NexusPublishExtension>().apply {
		clientTimeout.set(Duration.ofMinutes(20))
		repositories {
			sonatype {
				username.set(ossrhUsername)
				password.set(ossrhPassword)
			}
		}
	}
} else {
	apply<MavenPublishPlugin>()
}

if (hasSigningProps && !isSnapshot) {
	apply<SigningPlugin>()
	the<SigningExtension>().apply {
		isRequired = true
		useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
	}
	the<PublishingExtension>().apply {
		publications.withType<MavenPublication>().configureEach {
			the<SigningExtension>().sign(this)
		}
	}
}

val javadocJar = tasks.findByName("javadocJar") ?: tasks.create("javadocJar", Jar::class) {
	archiveClassifier.value("javadoc")
	// TODO: instead of a single empty Javadoc JAR, generate real documentation for each module
}

fun Node.add(key: String, value: String) {
	if ((get(key) as groovy.util.NodeList).isNotEmpty()) return // Node already exists
	appendNode(key).setValue(value)
}

fun Node.node(key: String, content: Node.() -> Unit) {
	appendNode(key).also(content)
}

the<PublishingExtension>().apply {
	require(ExternalResourceResolver.disableExtraChecksums()) { "Sonatype cannot handle extra checksums. "}
	publications.withType<MavenPublication>().configureEach {
		artifact(javadocJar)

		pom.withXml {
			asNode().apply {
				add("description", "Acorn UI libraries for multi-platform, OpenGL-based, applications and games.")
				add("name", "Acorn UI Library - ${project.name}")

				//this.nodes().containsKey()
				add("url", "https://github.com/polyforest/acornui")
				node("organization") {
					add("name", "Poly Forest, LLC")
					add("url", "https://www.polyforest.com")
				}
				node("issueManagement") {
					add("system", "github")
					add("url", "https://github.com/polyforest/acornui/issues")
				}
				node("licenses") {
					node("license") {
						add("name", "Apache License 2.0")
						add("url", "https://github.com/polyforest/acornui/blob/master/LICENSE")
						add("distribution", "repo")
					}
				}
				node("scm") {
					add("url", "https://github.com/polyforest/acornui-demos")
					add("connection", "scm:git:git://github.com/polyforest/acornui-demos.git")
					add("developerConnection", "scm:git:ssh://github.com/polyforest/acornui-demos.git")
				}
				node("developers") {
					node("developer") {
						add("name", "Nicholas Bilyk")
					}
				}
			}
		}
	}
}
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

buildscript {
	repositories {
		maven("https://plugins.gradle.org/m2/")
	}
	dependencies {
		classpath("de.marcphilipp.gradle:nexus-publish-plugin:0.3.0")
	}
}

//apply<MavenPublishPlugin>()
apply<SigningPlugin>()
apply<de.marcphilipp.gradle.nexus.NexusPublishPlugin>()

// Thanks: https://github.com/h0tk3y/k-new-mpp-samples/blob/master/publish-to-maven-central/build.gradle.kts

val javadocJar = tasks.findByName("javadocJar") ?: tasks.create("javadocJar", Jar::class) {
	archiveClassifier.value("javadoc")
	// TODO: instead of a single empty Javadoc JAR, generate real documentation for each module
}

// The root publication also needs a sources JAR as it does not have one by default

val sourcesJar = tasks.findByName("sourcesJar") ?: tasks.create("sourcesJar", Jar::class) {
	archiveClassifier.value("sources")
}

fun Node.add(key: String, value: String) {
	if ((get(key) as groovy.util.NodeList).isNotEmpty()) return // Node already exists
	appendNode(key).setValue(value)
}

fun Node.node(key: String, content: Node.() -> Unit) {
	appendNode(key).also(content)
}

val Project.isSnapshot: Boolean
	get() = version.toString().endsWith("-SNAPSHOT")

the<PublishingExtension>().apply {
	publications.withType<MavenPublication>().configureEach {
		if (name == "kotlinMultiplatform") {
			artifact(sourcesJar)
		}
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
		if (!isSnapshot)
			the<SigningExtension>().sign(this)
	}
}

the<SigningExtension>().apply {
	isRequired = !isSnapshot

	val signingKeyId: String by project
	val signingKey: String by project
	val signingPassword: String by project
	useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
}

//the<de.marcphilipp.gradle.nexus.NexusPublishExtension>().apply {
//	clientTimeout.set(java.time.Duration.ofMinutes(20))
//	repositories {
//		sonatype {
//			val ossrhUsername: String? by project
//			val ossrhPassword: String? by project
//			username.set(ossrhUsername)
//			password.set(ossrhPassword)
//		}
//	}
//}
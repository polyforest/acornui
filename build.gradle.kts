import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.SftpATTRS
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult


/*
 * Copyright 2019 Poly Forest, LLC
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

val kotlinJvmTarget: String by extra
val kotlinLanguageVersion: String by extra

plugins {
	kotlin("multiplatform")
	`maven-publish`
	idea
}

buildscript {
	dependencies {
		"classpath"(group = "com.jcraft", name = "jsch", version = "0.1.55")
	}
}


subprojects {
	apply {
		plugin("org.gradle.maven-publish")
	}
}

allprojects {
	apply {
		plugin("org.gradle.idea")
	}
	repositories {
		jcenter()
	}

	tasks.withType<KotlinCompile> {
		kotlinOptions {
			languageVersion = kotlinLanguageVersion
			apiVersion = kotlinLanguageVersion
		}
	}

	configurations.all {
		resolutionStrategy.dependencySubstitution.all {
			requested.let { r ->
				if (r is ModuleComponentSelector && r.group == group) {
					arrayOf("", "tools:", "backends:").firstNotNullResult {
						findProject(":$it${r.module}")
					}?.let { targetProject ->
						useTarget(targetProject)
					}
				}
			}
		}
	}

	val acornUiGradlePluginRepository: String? by extra
	if (acornUiGradlePluginRepository != null) {
		publishing {
			repositories {
				maven {
					url = uri(acornUiGradlePluginRepository!!)
				}
			}
		}

		tasks.register<Delete>("cleanSnapshots") {
			doLast {
				logger.lifecycle("Deleting old snapshots ${project.name} ${project.version}")
				delete(fileTree(acornUiGradlePluginRepository!!).matching {
					include("**/com/acornui/${project.name}-*/${project.version}/**")
				})
			}
		}

		tasks.publish.configure {
			dependsOn("cleanSnapshots")
		}
	}
}


tasks.register("publishReports") {
	group = "publishing"
	doLast {
		jschSftp("bandbox.dreamhost.com") { channel ->
			val reportDir = "testreports.acornui.com"
			channel.deleteRecursively(reportDir)
			subprojects.forEach { subProject ->
				val reports = subProject.buildDir.resolve("reports")
				if (reports.exists()) {
					channel.uploadDir(reports, "$reportDir/${subProject.name}")
					logger.lifecycle("Published unit test reports to http://testreports.acornui.com/${subProject.name}/tests/jvmTest/")
				}
			}
		}
	}
}

fun jschSftp(host: String, inner: (channel: ChannelSftp)->Unit) {
	val jsch = JSch()
	val ftpUsername: String = System.getenv("BANDBOX_FTP_USERNAME")
	val ftpPassword: String = System.getenv("BANDBOX_FTP_PASSWORD")

	val session = jsch.getSession(ftpUsername, host)
	session.setConfig("StrictHostKeyChecking", "no")
	session.setPassword(ftpPassword)
	session.connect()
	val channel = session.openChannel("sftp") as ChannelSftp
	channel.connect()
	try {
		inner(channel)
	} finally {
		channel.disconnect()
		session.disconnect()
	}
}

fun ChannelSftp.deleteRecursively(remoteDir: String) {
	if (isDir(remoteDir)) {
		ls(remoteDir).forEach { entry ->
			entry as ChannelSftp.LsEntry
			if (!(entry.filename == "." || entry.filename == "..")) {
				val file = "$remoteDir/${entry.filename}"
				if (entry.attrs.isDir) {
					deleteRecursively(file)
				} else {
					rm(file)
				}
			}
		}
		rmdir(remoteDir)
	}
}

fun ChannelSftp.mkdirs(destination: String) {
	var path = ""
	destination.split("/").forEach {
		path += it
		if (!exists(path)) {
			mkdir(path)
		}
		path += "/"
	}
}

fun ChannelSftp.uploadDir(file: File, destination: String) {
	if (!file.exists()) return
	val children = file.listFiles()!!
	mkdirs(destination)
	children.forEach {
		val childDestination = "$destination/${it.name}"
		if (it.isDirectory) {
			uploadDir(it, childDestination)
		} else {
			mkdirs(destination)
			put(it.path, childDestination)
		}
	}
}

fun ChannelSftp.exists(dir: String): Boolean {
	return statOrNull(dir) != null
}

fun ChannelSftp.isDir(dir: String): Boolean {
	return statOrNull(dir)?.isDir == true
}

fun ChannelSftp.statOrNull(dir: String): SftpATTRS? {
	return try {
		stat(dir)
	} catch (e: Throwable) { null }
}
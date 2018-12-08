/*
 * Copyright 2018 Poly Forest, LLC
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

import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.plugin.*
import com.liferay.gradle.plugins.node.NodeExtension
import com.liferay.gradle.plugins.node.tasks.ExecuteNodeTask
import com.liferay.gradle.plugins.node.tasks.DownloadNodeModuleTask
import com.liferay.gradle.plugins.node.tasks.ExecuteNodeScriptTask
import com.liferay.gradle.plugins.node.tasks.ExecuteNpmTask
import org.gradle.api.internal.file.AbstractFileCollection
import org.gradle.api.internal.tasks.DefaultTaskDependency
import kotlin.reflect.jvm.jvmName

buildscript {
	val KOTLIN_VERSION: String by gradle.startParameter.projectProperties

	repositories {
		maven(uri("https://plugins.gradle.org/m2/"))
		mavenCentral()
	}

	dependencies {
		classpath(kotlin("gradle-plugin:$KOTLIN_VERSION"))
		// TODO - MP: Use project property
		classpath("gradle.plugin.com.liferay:gradle-plugins-node:4.4.2")
	}
}

val acornConfig: Map<String, String> = gradle.startParameter.projectProperties
val polyforestProjectFlag = "POLYFOREST_PROJECT"
val ACORNUI_HOME by acornConfig
val acornUiHome = file(ACORNUI_HOME)
val APP_HOME = acornConfig["APP_HOME"]

// General helpers
// sourceSets is being seen as private in applied scripts, so locally defining accessor here
/**
 * Retrieves the [sourceSets][org.gradle.api.tasks.SourceSetContainer] extension.
 */
val Project.`sourceSets`: SourceSetContainer
	get() =
		(this as ExtensionAware).extensions.getByName("sourceSets") as SourceSetContainer

val Gradle.isComposite
	get() = includedBuilds.isNotEmpty()
val Gradle.isNestedComposite
	get() = isComposite && !isRoot
val Project.isComposite
	get() = gradle.isComposite
val Gradle.isRoot
	get() = parent == null
val Project.isRoot
	get() = parent == null
val Project.isCompositeRoot
	get() = gradle.isRoot && isComposite && isRoot
val Project.isTopLevelIncludedBuild
	get() = gradle.parent?.let { it.isComposite && it.isRoot } ?: false

val Project.isAcornUiRoot
	get() = projectDir.canonicalPath == acornUiHome.canonicalPath
val Project.isAcornUiProject
	get() = rootProject.isAcornUiRoot

val Project.isAcornUi: Boolean by lazy {
	projectDir.canonicalPath == acornUiHome.canonicalPath
}
val Project.isPolyForestProject: Boolean by lazy {
	(this.hasProperty(polyforestProjectFlag) && this.property(polyforestProjectFlag).toString().toBoolean())
}

/**
 * Designates task to only be run if the build is a prod build.
 *
 * @see htmlSrcDestPathByBuildType as an example of dealing with hybrid tasks that execute for both builds but have
 * differing configuration.
 */
inline fun <T : Task> T.releaseTask() {
	// So that the task can be identified as a release task...
	val releaseTask by extra(true)

	var isProdBuild: Property<Boolean> by rootProject.extra
	// If a lambda is passed to onlyIf, the block will be evaluated immediately.
	onlyIf(closureOf<Task> { isProdBuild.get() })
}

fun Project.isThatModule(module: String) = this.name == module

fun Project.isAppModule(): Boolean {
	val APP_HOME = gradle.startParameter.projectProperties["APP_HOME"]
	return APP_HOME?.let { this.rootDir.canonicalPath == it } ?: false
}

/**
 * Calls the specified function [block] and returns Unit.
 *
 * The requirement for some enclosing lambdas to return Unit can dictate the order of execution of the block, given
 * the last call in the block must return Unit itself.
 *
 * This helper is useful for wrapping any function call that accepts a lambda as its last parameter to ensure it returns
 * unit.
 *
 * ex:  By wrapping `extra` property lambda bodies, consumers don't need to worry about where the property lambda is
 * called.
 */
inline fun toUnit(block: () -> Any?) {
	block()
	return
}

/**
 * Returns the specified function [block] wrapped in a lambda expression which returns Unit.
 *
 * Cannot protect against non-local returns inside of the function [block].
 */
inline fun toUnitLambda(crossinline block: () -> Any?): () -> Unit {
	return {
		block()
		Unit
	}
}

val TARGET_JVM_VERSION by acornConfig
val ACORNUI_GROUP by acornConfig
val acornAllProjects by extra { p: Project ->
	toUnit {
		with(p) {
			if (isPolyForestProject)
				group = ACORNUI_GROUP
			repositories {
				mavenCentral()
			}
		}
	}
}

configurations.all {
	resolutionStrategy {
		preferProjectModules()
	}
}

/**
 * Task requests are created at a build level.  Composite builds must manually define proxy tasks to call tasks inside
 * included builds.  These target tasks, upon which properties indicating that they are production oriented are set,
 * cannot be guaranteed to have the same name as their proxy tasks.
 */
fun resolveTaskRequestTarget(parent: Task?, task: Task?): List<Task> {
	val targetTasks = mutableListOf<Task>()

	return if (task == null || task.project == parent?.project)
		targetTasks
	else if (task.project.subprojects.isEmpty())
		targetTasks.also { it.add(task) }
	else
		task.taskDependencies.getDependencies(null).flatMap { resolveTaskRequestTarget(task, it) }
}

fun findAncestorRequestedTaskNames(gradle: Gradle?): List<String> {
	val taskNames = gradle?.startParameter?.taskNames

	return if (gradle == null)
		emptyList()
	else if (!taskNames.isNullOrEmpty())
		taskNames
	else
		findAncestorRequestedTaskNames(gradle.parent)
}

fun findRequestedTaskNames(project: Project): List<String>? {
	return findAncestorRequestedTaskNames(project.gradle)
}

fun findTask(project: Project?, taskName: String): Task? {
	val property = project?.findProperty(taskName)

	return if (project == null)
		null
	else if (property != null && property is Task)
		property
	else
		findTask(project.parent, taskName)
}

fun resolveTaskRequestTargets(project: Project): List<Task>? {
	return findRequestedTaskNames(project)?.flatMap {
		resolveTaskRequestTarget(null, findTask(project, it))
	}
}

var isProdBuild by extra(project.objects.property<Boolean>())

/**
 * Determines and sets whether the build is a production js build or dev.
 * Must be called with js project.
 */
fun setBuildType(project: Project) {
	toUnit {
		with(project) {
			// After all tasks have been registered/created and tagged as release tasks (as applicable)...
			afterEvaluate {
				val releaseTaskPropName = "releaseTask"
				var isProdBuild: Property<Boolean> by rootProject.extra

				val noRequestedReleaseTasks =
					resolveTaskRequestTargets(project)?.all { task ->
						!task.hasProperty(releaseTaskPropName)
					} ?: true

				if (noRequestedReleaseTasks)
					isProdBuild.set(false)
				else
					isProdBuild.set(true)
			}
		}
	}
}

if (isTopLevelIncludedBuild) {
	afterEvaluate {
		tasks {
			val buildAll by creating(DefaultTask::class) {
				group = "other-helpers"
				description = "Build all subprojects."
				dependsOn(getTasksByName("build", true))
			}

			val cleanAll by creating(Delete::class) {
				group = "other-helpers"
				description = "Delete build directories of all subprojects."
				dependsOn(getTasksByName("clean", true))
			}

			val fatJarAll by creating(Jar::class) {
				group = "other-helpers"
				description = "Builds single jar files including all dependencies and resources contained in all " +
						"subprojects."
				dependsOn(getTasksByName("fatJar", true))
			}
		}
	}
}

val commonProjectConfiguration by extra { p: Project ->
	toUnit {
		with(p) {
			dependencies {
				"implementation"(kotlin("stdlib-common"))

				"testImplementation"(kotlin("test-common"))
				"testImplementation"(kotlin("test-annotations-common"))
			}
		}
	}
}

val jsProjectConfiguration by extra { p: Project ->
	toUnit {
		with(p) {
			dependencies {
				"implementation"(kotlin("stdlib-js"))

				"testImplementation"(kotlin("test-js"))
			}

			declareResourceGenerationTasks(this)
			declareJsEntryPointConfiguration(this)
		}
	}
}

val jvmProjectConfiguration by extra { p: Project ->
	toUnit {
		with(p) {
			dependencies {
				"implementation"(kotlin("stdlib-jdk8"))

				"testImplementation"(kotlin("test"))
				"testImplementation"(kotlin("test-junit"))
			}
			declareResourceGenerationTasks(this)
		}
	}
}

val maybeCreateBuildutilsConfiguration by extra { p: Project ->
	lateinit var buildutils: Configuration
	with(p) {
		buildutils = configurations.maybeCreate("buildutils")
		dependencies {
			buildutils("$ACORNUI_GROUP:acornui-buildutils")
		}
	}
	buildutils
}

/**
 * Provides traceability between a directory file (key), its configurable file collection (live and lazy contents)
 * (value), and the tasks that build the contents (tracked in the value instance).
 *
 * Tasks that depend on a configurable file collection will implicitly depend on all tasks that build it.  This helps
 * downstream build engineers hook custom build logic into the task graph in a non-brittle way.
 *
 * Utilized for directories later registered as an output of a source set as the tasks that build it must be known at
 * registration time.
 *
 * All path strings are validated and resolved according to Project.files()
 */
object DirCollection {

	val dirs = mutableMapOf<File, ConfigurableFileCollection>()

	/**
	 * If a [collection] is provided, registers or overwrites an existing entry for [path] and returns the [collection].
	 * If a [collection] is not provided, the registered ConfigurableFileCollection for the [path] is returned.
	 * If there is no ConfigurableFileCollection for the [path] and a [collection] is not provided, an Exception is
	 * thrown.
	 *
	 * Note:  The ConfigurableFileCollection instance cannot be constructed within this object due to the Project
	 * service being out of scope.
	 */
	fun dir(
		project: Project,
		path: String,
		collection: ConfigurableFileCollection? = null
	): ConfigurableFileCollection {

		val file = project.file(path)
		if (collection != null)
			dirs[file] = collection
		else if (dirs[file] == null) {
			throw Exception(
				"The file collection for the path, ${file.canonicalPath}, does not exist in the " +
						"DirCollection map."
			)
		}

		return dirs[file]!!
	}

	/**
	 * Registers a [Task] receiver instance as being a builder of the directory that matches the [path].
	 *
	 * If the directory is not in [dirs], the [path] is empty, or the [path] is '/', nothing is done.
	 */
	val builds = fun Task.(path: String) {
		val file = project.file(path)

		dirs[file]?.builtBy(this)
		return
	}
}

/**
 * Registers a task as building a directory.
 *
 * Syntax sugar for `DirCollection.builds` to keep it safely scoped.
 */
fun <T : Task> T.buildsDir(path: String) {
	DirCollection.builds(this, path)
}

// TODO - MP: Store as map of directory properties (project.objects.dir()) associated to the configurable file collection
/**
 * Registers a new directory file with its configurable file collection.
 *
 * Syntax sugar for `DirCollection.dir` to keep it safely scoped.
 */
fun Project.dir(directory: String, collection: ConfigurableFileCollection? = null): ConfigurableFileCollection =
	DirCollection.dir(this, directory, collection)

/**
 * Returns the FileVisitDetails of the direct children of [fileTree]'s roots as a List.
 *
 * Does not resolve [fileTree].
 * @see FileVisitDetails for more information.
 */
fun FileTree.getRootsChildren(): List<FileVisitDetails> {
	val rootsChildren = mutableListOf<FileVisitDetails>()

	visit {
		val fileVisitDetails = this
		// relativePath.parent has no equality method
		if (relativePath.segments.size == 1) {
			rootsChildren.add(fileVisitDetails)
		}
	}

	return rootsChildren.toList()
}

/**
 * Returns a List of Ant-pattern Strings for inclusion or exclusion in CopySpecs based on [fileDetails].
 * FileVisitDetails can be gathered by visiting a FileTree.
 *
 * @see FileTree.getRootsChildren
 * @see FileTree.visit
 */
fun generatePatterns(fileDetails: List<FileVisitDetails>): List<String> {
	return fileDetails.map {
		if (it.isDirectory)
			"${it.name}/**"
		else
			it.name
	}
}

/**
 * Destination directory root for generated/processed resources.
 */
fun generatedResourceTaskPath(project: Project, taskName: String? = null): String {

	val generatedResourceBasePath = "${project.buildDir.canonicalPath}/generated-resources/main"

	return "$generatedResourceBasePath${taskName?.let { "/$it" } ?: ""}"
}

/**
 * Destination for development html sources.
 */
fun htmlSrcDestPath(project: Project) = "${generatedResourceTaskPath(project)}/htmlSrc"

/**
 * Destination for production html sources.
 */
fun prodHtmlSrcDestPath(project: Project) = "${generatedResourceTaskPath(project)}/htmlSrcDist"

/**
 * Return the proper html source path depending on the build type (prod/dev).
 */
fun htmlSrcDestPathByBuildType(project: Project): String {

	var isProdBuild: Property<Boolean> by rootProject.extra

	return if (isProdBuild.get())
		prodHtmlSrcDestPath(project)
	else
		htmlSrcDestPath(project)
}

/**
 * Destination for packed assets.
 */
fun processedResourcesPath(project: Project) = "${generatedResourceTaskPath(project)}/assetSrc"

/**
 * Path to processed (static or post-compiled) source.
 * e.g. processed dev or production html source for Js would go here.
 */
fun processedSourcePath(project: Project): String? {

	return if (isThatModule("${rootProject.name}-jvm"))
		null
	else
		htmlSrcDestPathByBuildType(project)
}

/**
 * Destination for finalized resources (processed and unprocessed).
 *
 * Note:  Js backend combines sources and resources into this directory for serving the application statically on a
 * local dev web server.
 */
fun finalResourcesPath(project: Project): String = "${project.buildDir}/finalResources/main"

/**
 * Resource processing and generation for jvm and js backends.
 */
val declareResourceGenerationTasks by extra { p: Project ->

	toUnit {
		with(p) {
			fun Project.isApplicationEntryPointModule(): Boolean {
				return isAppModule() &&
						(isThatModule("${rootProject.name}-jvm") || isThatModule("${rootProject.name}-js"))
			}

			if (isApplicationEntryPointModule()) {

				val main by sourceSets
				val buildutils = maybeCreateBuildutilsConfiguration(p)

				val processedResourcesPath by extra(processedResourcesPath(p))
				val finalResourcesPath by extra(finalResourcesPath(p))

				/**
				 * Register the destination for processed resources.  By default, these are all declared resource source
				 * directories declared within the configuration phase.
				 *
				 * e.g. selected and processed skin + `.../main/src/resources`
				 */
				dir(processedResourcesPath, files(processedResourcesPath)).let {
					afterEvaluate {
						main.output.dir(mapOf("builtBy" to it.builtBy), processedResourcesPath)
					}
				}

				setBuildType(p)

				tasks {

					val stageResources by creating(Copy::class) {
						description = "Copy resources into staging directory for further processing prior to ending " +
								"up on the classpath."

						// TODO - MP: Make sure skin is easily selectable, maybe move up into a main sourceSets block?
						// TODO - MP: Ensure that resources will always take precedent over /skin in merge to allow for skin tweaks.
						// Make sure skin is included.
						val skin = file("${acornConfig["APP_SKIN_DIR"]}/resources")
						main.resources.srcDir(skin)

						from(main.resources.sourceDirectories)
						into(processedResourcesPath)

						/**
						 * Prevent both unprocessed and processed resources from co-existing on the classpath (or copied
						 * around more than necessary).
						 *
						 * `main.output.resources.sourceDirectories` is still used by other build logic so it is important to
						 * use this method of exclusion.  It also preserves any extra sources consumers may attach to the task
						 * itself as inputs.
						 **/
						val processResources by getting(Copy::class)

						// Since stageResources preserves processResources inclusions/exclusions, stageResources
						// inclusions/exclusions must come first.
						include(processResources.includes)
						exclude(processResources.excludes)

						// Exclude all declared resource directories in the standard processResources task which simply
						// copies resources by default.
						val resourceDirsDirectChildren =
							main.resources.sourceDirectories.asFileTree.getRootsChildren()
						processResources.exclude(generatePatterns(resourceDirsDirectChildren))

						buildsDir(processedResourcesPath)
					}

					val packAssets by creating(JavaExec::class) {
						description = "Pack and gather assets for ${project.name.substringBeforeLast('-')}'s " +
								"${project.name.substringAfterLast('-')} module."

						val srcDir by extra(stageResources.destinationDir)

						inputs.dir(srcDir)
						inputs.property("src", srcDir)

						outputs.dir(srcDir).withPropertyName("outputFiles")

						args = listOf(
							"-target=assets",
							"-src=${inputs.properties["src"]}"
						)
						this.main = "com.acornui.build.BuildUtilKt"
						classpath = buildutils

						buildsDir(processedResourcesPath)
						dependsOn(buildutils, stageResources)
					}

					val createResourceFileManifest by creating(JavaExec::class) {
						description = "Generate a file manifest of shared resources for file handling at runtime."

						val srcDir by extra(processedResourcesPath)
						val destinationDir by extra("$srcDir/assets")
						with(inputs) {
							// Directory path containing resources to be targeted by the manifest.
							property("src", srcDir)
							// Directory path from which relative paths to resources are generated.
							property("root", srcDir)
							property("dest", destinationDir)
							files(srcDir)
						}

						outputs.file("$destinationDir/files.json")

						args = listOf(
							"-target=asset-manifest",
							"-src=${inputs.properties["src"]}",
							"-dest=${inputs.properties["dest"]}",
							"-root=${inputs.properties["root"]}"
						)
						this.main = "com.acornui.build.BuildUtilKt"
						classpath = buildutils

						buildsDir(processedResourcesPath)
						dependsOn(packAssets)
					}

					val processGeneratedResources by creating(DefaultTask::class) {
						description = """
						Lifecycle task to attach generated resource processing tasks (contains configuration phase logic)
						In execution => does nothing
						In configuration => computes set of generated resource directories to be on the final main classpath
						Depends on => Directly/indirectly depends on all tasks contributing to resource generation
					""".trimIndent()

						/**
						 * Allow other tasks or configuration to add to the file collection during the configuration
						 * phase.
						 *
						 * e.g.
						 * val processGeneratedResources by project.tasks.extra
						 * val generatedResourceSources: ConfigurableFileCollection by processGeneratedResources.extra
						 * generatedResourceSources.add(<see ConfigurableFileCollection.add for valid params>)
						 */
						val generatedResourceSources by extra(dir(processedResourcesPath))

						dependsOn(generatedResourceSources)
					}

					val processFinalResources = maybeCreate("processFinalResources", Copy::class)
					processFinalResources.apply {
						val processResources by project.tasks.getting(Copy::class)
						val destinationDir by extra(finalResourcesPath)

						description = "Copies generated (${processGeneratedResources.name} outputs) and non-generated " +
								"resources (${processResources.name} outputs) into $destinationDir."

						val generatedResourceSources: ConfigurableFileCollection by processGeneratedResources.extra

						from(processResources, generatedResourceSources)
						into(destinationDir)
					}

					val classes by getting {
						dependsOn(processFinalResources)
					}

					listOf(packAssets, createResourceFileManifest).forEach {
						it.logging.captureStandardOutput(LogLevel.INFO)
						it.logging.captureStandardError(LogLevel.INFO)
					}

					// TODO - MP: Deal with later
					val buildNumber = resources.text.fromFile("$rootDir/build.txt")
					val bumpBuildVersion by creating(DefaultTask::class) {
						enabled = false
					}
				}
			}
		}
	}
}

// Js helpers.
object NpmDependencies {

	val dependencies = mapOf("http-server" to "0.11.1", "uglify-js" to "3.4.9")

	fun taskNameSuffix(dependency: Map.Entry<String, String>): String {
		return taskNameSuffix(dependency.key)
	}

	fun taskNameSuffix(npmModule: String): String {
		return npmModule.toLowerCase().split('-').joinToString("") { it.capitalize() }
	}
}

val npmRunPrefix by extra("npmRun")

inline fun <reified T : Task> npmGetting(project: Project, npmModule: String, noinline configuration: T.() -> Unit) {

	val taskName = npmRunPrefix + NpmDependencies.taskNameSuffix(npmModule)

	return project.tasks.withType(T::class).named(taskName).configure(configuration)
}

typealias FileProcessor = (src: String, file: File) -> String

object SourceFileManipulator {

	/**
	 * Iterates through [files] and applies [processors] to their contents in place.
	 */
	fun <T : Iterable<File>> process(files: T, processors: List<FileProcessor>) {
		files.forEach {
			if (it.isFile) {
				var src = it.readText()
				for (processor in processors) {
					src = processor(src, it)
				}
				it.writeText(src)
			}
		}
	}

	/**
	 * Iterates through [files] and applies [processor] to their contents in place.
	 */
	fun <T : Iterable<File>> process(files: T, processor: FileProcessor) {
		process(files, listOf(processor))
	}
}

object ScriptCacheBuster {

	val extensions = listOf(
		"asp", "aspx", "cshtml", "cfm", "go", "jsp", "jspx", "php", "php3", "php4", "phtml", "html", "htm", "rhtml",
		"css"
	)

	private val regex = Regex("""([\w./\\]+)(\?[\w=&]*)(%VERSION%)""")

	/**
	 * Replaces %VERSION% tokens with the last modified timestamp.
	 * The source must match the format:
	 * foo/bar.ext?baz=%VERSION%
	 * foo/bar.ext must be a local file.
	 */
	fun replaceVersionWithModTime(src: String, file: File): String {
		return regex.replace(src) { match ->
			val path = match.groups[1]!!.value
			val relativeFile = File(file.parent, path)
			if (relativeFile.exists()) path + match.groups[2]!!.value + relativeFile.lastModified()
			else match.value
		}
	}
}

object KotlinMonkeyPatcher {

	/**
	 * Makes it all go weeeeee!
	 */
	fun optimizeProductionCode(src: String, file: File? = null): String {
		var result = src
		result = simplifyArrayListGet(result)
		result = stripCce(result)
		result = stripRangeCheck(result)
		result += "function alwaysTrue() { return true; }"
		return result
	}

	/**
	 * Strips type checking that only results in a class cast exception.
	 */
	private fun stripCce(src: String): String {
		val d = '$'
		return Regex("""Kotlin\.is(Type|Array|Char|CharSequence|Number)(\((.*?) \? tmp\$d :
			|(Kotlin\.)?throw(\S*)\(\))""".trimMargin()).replace(src, "alwaysTrue\$2")
	}

	private fun stripRangeCheck(src: String): String {
		return src.replace("this.rangeCheck_2lys7f${'$'}_0(index)", "index")
	}

	private fun simplifyArrayListGet(src: String): String {
		return Regex("""ArrayList\.prototype\.get_za3lpa\$[\s]*=[\s]*function[\s]*\(index\)[\s]*\{([^}]+)};""").replace(
			src
		) {
			"""ArrayList.prototype.get_za3lpa$ = function(index) { return this.array_hd7ov6${'$'}_0[index] };"""
		}
	}

	private fun simplifyArrayListSet(src: String): String {
		return Regex("""ArrayList\.prototype\.set_wxm5ur\$[\s]*=[\s]*function[\s]*\(index, element\)[\s]*\{([^}]+)};""").replace(
			src
		) {
			"""ArrayList.prototype.set_wxm5ur${'$'} = function (index, element) {
			  var previous = this.array_hd7ov6${'$'}_0[index];
			  this.array_hd7ov6${'$'}_0[index] = element;
			  return previous;
			};"""
		}
	}
}

// TODO - MP: Document
operator fun TaskDependency.minus(taskDependency: TaskDependency): TaskDependency {
	val result = DefaultTaskDependency()
	val newDependencies = this.getDependencies(null) - taskDependency.getDependencies(null)
	return result.add(*newDependencies.toTypedArray())
}

fun FileCollection.minusAll(collection: FileCollection): FileCollection {
	if (this is AbstractFileCollection)
		return minusAll(collection)
	else
		throw Exception("$this does not implement or extend AbstractFileCollection.")
}

// TODO - MP: Document
fun <T : AbstractFileCollection> T.minusAll(fileCollection: FileCollection): FileCollection {
	val outerThis = this

	return object : AbstractFileCollection() {
		override fun getDisplayName(): String {
			return outerThis.displayName
		}

		override fun getBuildDependencies(): TaskDependency {
			return outerThis.buildDependencies - fileCollection.buildDependencies
		}

		override fun getFiles(): Set<File> {
			val files = LinkedHashSet<File>(outerThis.files)
			files.removeAll(fileCollection.files)
			return files
		}

		override fun contains(file: File): Boolean {
			return outerThis.contains(file) && !fileCollection.contains(file)
		}
	}
}

/**
 * Retrieves the [node][com.liferay.gradle.plugins.node.NodeExtension] extension.
 */
val org.gradle.api.Project.`node`: com.liferay.gradle.plugins.node.NodeExtension
	get() = (this as org.gradle.api.plugins.ExtensionAware).extensions.getByType()

// TODO - MP: Document
fun <T : Task, S : FileCollection> T.subtractFromDependsOn(collection: S): Set<Any> {
	val dependsOnMutatedElements = mutableSetOf<Any>()
	val otherDependencies =
		collection.buildDependencies.getDependencies(null)

	val strippedDependsOn = dependsOn.toMutableSet()
	strippedDependsOn.removeIf { dependency: Any ->
		when (dependency) {
			is AbstractFileCollection -> {

				if (dependency != collection)
					dependsOnMutatedElements.add(dependency.minusAll(collection))

				true
			}

			is Task                   -> otherDependencies.contains(dependency)

			is TaskDependency         -> {
				val dependencies = dependency.getDependencies(null)
				when {
					dependencies == otherDependencies                   -> true

					dependencies.any { otherDependencies.contains(it) } -> {
						dependsOnMutatedElements.add(dependency - collection.buildDependencies)

						true
					}

					else                                                -> false
				}
			}

			else                      -> {
				// TODO - MP: Shore up remaining types.
				logger.log(
					LogLevel.DEBUG, """
					This dependency's type is not supported, so it wasn't removed from the file collection:
						${dependency::class.jvmName} - $dependency
				""".trimIndent()
				)

				false
			}
		}
	}

	return (strippedDependsOn + dependsOnMutatedElements)
}

/**
 * Configuration for a module that serves as a Js entry point for the acorn app (i.e. consuming app).
 */
val declareJsEntryPointConfiguration by extra { p: Project ->

	toUnit {
		with(p) {
			if (isAppModule() && isThatModule("${rootProject.name}-js")) {
				val htmlSrcDestPath by extra(htmlSrcDestPath(p))
				val prodHtmlSrcDestPath by extra(prodHtmlSrcDestPath(p))

				listOf(htmlSrcDestPath, prodHtmlSrcDestPath).forEach {
					dir(it, files(it))
				}

				val main: SourceSet by sourceSets
				var isProdBuild: Property<Boolean> by rootProject.extra

				/**
				 * Setup the final JS html source to be used (prod or dev).
				 */
				val finalHtmlSrcDest by extra(objects.directoryProperty())
				// TODO - MP: Spike if possible to refactor runtimeCollection to use KotlinClosure0 and a Provider
				// 	property
				var runtimeCollection by files()

				afterEvaluate {
					val htmlSrcPath = if (!isProdBuild.get()) htmlSrcDestPath else prodHtmlSrcDestPath
					finalHtmlSrcDest.set(file(htmlSrcPath))

					dir(htmlSrcPath).let { finalHtmlSrc ->

						main.output.dir(mapOf("builtBy" to finalHtmlSrc.builtBy), htmlSrcPath)

						// Remove circular dependency in main's classpaths while retaining processed/generated
						// resources in artifacts.
						val processFinalResources by tasks.getting
						val processFinalProdResources by tasks.getting

						val classes by tasks.getting {
							setDependsOn(subtractFromDependsOn(finalHtmlSrc) - processFinalResources)
						}

						// Ensure the source still ends up in the jar.
						val assemble by tasks.getting {
							dependsOn(processFinalResources, processFinalProdResources)
						}

						// Setup runtime collection to not include resource circular dependencies.
						runtimeCollection.apply {
							val target =
								(main.runtimeClasspath + main.output.classesDirs).minusAll(finalHtmlSrc)

							from(KotlinClosure0(fun() = target))
							builtBy(*target.buildDependencies.getDependencies(null).toTypedArray())
						}
					}
				}

				val acornConfig: MutableMap<String, String> = gradle.startParameter.projectProperties

				// TODO - MP: Migrate to global properties?
				acornConfig["JS_LIBS_DEST"] = "lib"
				acornConfig["HTML_SRC_PATH"] = "${projectDir.canonicalPath}/src/main/web"
				val libsDest = acornConfig["JS_LIBS_DEST"]!!
				val htmlSrc = acornConfig["HTML_SRC_PATH"]!!

				tasks {
					val buildutils = maybeCreateBuildutilsConfiguration(p)


					val assembleWebSource by creating(Copy::class) {
						description = "Gather html (.html, .js, etc) source for the ${project.name} module and its " +
								"dependencies (sans resources)."

						val jsSrcAndMaps = { path: String ->
							listOf(".js", ".js.map").any { path.endsWith(it) }
						}
						val allDirsSansDependencyMeta = { path: String ->
							path.startsWith("META-INF/resources/") || !path.startsWith("META-INF")
						}

						val currentCopyDetails = mutableListOf<FileCopyDetails>()

						into(htmlSrcDestPath)
						from(htmlSrc)
						exclude("**/*.meta.js")

						into(libsDest) {
							// Delay evaluation by passing a Closure.
							from(KotlinClosure0(fun() = runtimeCollection.map {
								if (it.extension in listOf("war", "jar", "zip"))
									zipTree(it)
								else
									it
							}))

							includeEmptyDirs = false
							include {
								jsSrcAndMaps(it.path) && allDirsSansDependencyMeta(it.path)
							}

							// Preserve last modified date for each file
							eachFile {
								currentCopyDetails + this
							}
						}

						//  Capture lastmodified to be used for input changes in other task
						val copyDetails by extra(currentCopyDetails)

						// Finish preserving last modified date for each file
						doLast {
							copyDetails.forEach { details ->
								File(destinationDir, details.path).setLastModified(details.lastModified)
							}
						}

						buildsDir(htmlSrcDestPath)
						dependsOn(runtimeCollection)
					}

					val stageWebSourceForProcessing by creating(Copy::class) {
						description =
								"Copy gathered html source into intermediate build directory for further processing."

						from(assembleWebSource)
						exclude("**/*.js.map")
						into(prodHtmlSrcDestPath)

						releaseTask()
						buildsDir(prodHtmlSrcDestPath)
					}

					val minifyJs by creating(SourceTask::class) {
						description = "Minify js."
						enabled = false

						source(stageWebSourceForProcessing)
						// TODO - MP: Add other target extensions.
						include("**/*.js")

						// TODO - MP: Finish processing portion - minify
						releaseTask()
						buildsDir(prodHtmlSrcDestPath)
					}

					val optimizeJs by creating(SourceTask::class) {
						description = "Monkeypatch js code with optimizations."

						source(prodHtmlSrcDestPath)
						include("**/*.js")

						doLast {
							SourceFileManipulator.process(source, KotlinMonkeyPatcher::optimizeProductionCode)
						}

						releaseTask()
						buildsDir(prodHtmlSrcDestPath)
						dependsOn(minifyJs)
					}

					val createJsFileManifest by creating(JavaExec::class) {
						group = "build"
						description = "Generate a file manifest of js libs for file handling at runtime."

						onlyIf(closureOf<Task> { !isProdBuild.get() })

						val destinationDir = htmlSrcDestPath

						with(inputs) {
							// Directory path containing resources to be targeted by the manifest.
							property("src", destinationDir)

							// Directory path from which relative paths to resources are generated.
							property("root", destinationDir)
							property("dest", "$destinationDir/$libsDest")
							files(properties["src"])
						}

						outputs.dir(destinationDir).withPropertyName("outputFiles")

						args = listOf(
							"-target=lib-manifest",
							"-src=${inputs.properties["src"]}",
							"-dest=${inputs.properties["dest"]}",
							"-root=${inputs.properties["root"]}"
						)
						this.main = "com.acornui.build.BuildUtilKt"
						classpath = buildutils

						buildsDir(destinationDir)
						dependsOn(assembleWebSource)
					}

					val bustHtmlSourceScriptCache by creating(SourceTask::class) {
						description = "Modify ${project.name} html sources to bust browser cache."

						val srcDir = htmlSrcDestPath
						source(srcDir)
						include { fileTreeElement ->
							!fileTreeElement.isDirectory && ScriptCacheBuster.extensions.any { extension ->
								fileTreeElement.name.endsWith(extension)
							}
						}

						doLast {
							SourceFileManipulator.process(source, ScriptCacheBuster::replaceVersionWithModTime)
						}

						buildsDir(srcDir)
						dependsOn(createJsFileManifest)
					}

					val processGeneratedResources by getting {
						val htmlSrcCollection by extra(dir(htmlSrcDestPath))
						dependsOn(htmlSrcCollection)
					}

					val processFinalResources by getting(Copy::class) {
						val htmlSrcCollection: ConfigurableFileCollection by processGeneratedResources.extra
						from(htmlSrcCollection)
					}

					val createProdJsFileManifest by creating(JavaExec::class) {
						group = "build"
						description = "Generate a file manifest of js libs for file handling at runtime."

						val destinationDir = prodHtmlSrcDestPath

						with(inputs) {
							// Directory path containing resources to be targeted by the manifest.
							property("src", destinationDir)

							// Directory path from which relative paths to resources are generated.
							property("root", destinationDir)
							property("dest", "$destinationDir/$libsDest")
							files(properties["src"])
						}

						outputs.dir(destinationDir).withPropertyName("outputFiles")

						args = listOf(
							"-target=lib-manifest",
							"-src=${inputs.properties["src"]}",
							"-dest=${inputs.properties["dest"]}",
							"-root=${inputs.properties["root"]}"
						)
						this.main = "com.acornui.build.BuildUtilKt"
						classpath = buildutils

						releaseTask()
						buildsDir(destinationDir)
						dependsOn(stageWebSourceForProcessing, optimizeJs)
					}

					val bustProdHtmlSourceScriptCache by creating(SourceTask::class) {
						description = "Modify ${project.name} html sources to bust browser cache."

						val srcDir = prodHtmlSrcDestPath
						source(srcDir)
						include { fileTreeElement ->
							!fileTreeElement.isDirectory && ScriptCacheBuster.extensions.any { extension ->
								fileTreeElement.name.endsWith(extension)
							}
						}

						doLast {
							SourceFileManipulator.process(source, ScriptCacheBuster::replaceVersionWithModTime)
						}

						releaseTask()
						buildsDir(srcDir)
						dependsOn(createProdJsFileManifest)
					}

					val generatedResourceSources: ConfigurableFileCollection by processGeneratedResources.extra
					val prodHtmlSrcCollection by extra(dir(prodHtmlSrcDestPath))

					val processGeneratedProdResources by creating(DefaultTask::class) {
						dependsOn(generatedResourceSources, prodHtmlSrcCollection)

						releaseTask()
					}

					// TODO - MP: Make this match processFinalResources.  Something is asymmetrical, though it works.
					// This will cause inconsistent expectations for downstream build engineers.
					val processFinalProdResources by creating(Copy::class) {
						destinationDir = file(finalResourcesPath(p))
						into(destinationDir)

						val processResources by getting
						from(processResources, generatedResourceSources, prodHtmlSrcCollection)

						into(file(finalResourcesPath(p)))

						releaseTask()
					}

					val run = maybeCreate("run", DefaultTask::class)
					run.dependsOn(processFinalResources)

					val runProd = maybeCreate("runProd", DefaultTask::class)
					runProd.apply {
						releaseTask()
						dependsOn(processFinalProdResources)
					}

					// TODO - MP: We should change this to a zip artifact when publishing.
					// Package jar just like the file-system deploy.
//					val jar by getting(Jar::class) {
//						val compiledOutputPatterns =
//							generatePatterns(main.output.classesDirs.asFileTree.getRootsChildren())
//						exclude(compiledOutputPatterns)
//					}
				}

				val printDevClasspath by tasks.creating(DefaultTask::class) {
					group = "build.debug"

					doLast {
						println(main.runtimeClasspath.joinToString("\n") { it.canonicalPath })
					}

					dependsOn(main.runtimeClasspath)
				}

				val printProdClasspath by tasks.creating(DefaultTask::class) {
					group = "build.debug"

					doLast {
						println(main.runtimeClasspath.joinToString("\n") { it.canonicalPath })
					}

					releaseTask()
					dependsOn(main.runtimeClasspath)
				}
			}
		}
	}
}

val ACORNUI_PLUGINS_AVAILABLE by acornConfig
val acornUiPlugins = ACORNUI_PLUGINS_AVAILABLE.split(",")

val allSansPluginsBuilds = gradle.includedBuilds.filterNot { it.name in acornUiPlugins }
val internalPluginsBuilds = gradle.includedBuilds.filter { it.name in acornUiPlugins }

if (isCompositeRoot) {
	tasks {
		val build by getting(DefaultTask::class) {
			allSansPluginsBuilds.forEach { dependsOn(it.task(":buildAll")) }
		}

		val clean by getting(Delete::class) {
			description = "Delete all non-acornui plugin build directories and the local acornui plugins repo."
			allSansPluginsBuilds.forEach { dependsOn(it.task(":cleanAll")) }
			internalPluginsBuilds.forEach { dependsOn(it.task(":cleanLocalPluginsRepo")) }
		}

		val buildPlugins by creating(DefaultTask::class) {
			group = "build"
			description = "Build locally built/applied plugins."
			/**
			 * Wrapper is the task IDEA runs to sync the root project.
			 * Composite builds automatically build local binary plugins modules that are depended upon when substituted
			 * for project dependencies.
			 */
			if (project.isAcornUiRoot)
			// Allow for building all plugins upon getting acorn source
				internalPluginsBuilds.forEach { dependsOn(it.task(":build")) }
			else
			// Only build those that the consumer app and composite build depend upon directly
				dependsOn(tasks["wrapper"])
		}

		val cleanPlugins by creating(Delete::class) {
			group = "build"
			description = "Delete all acornui plugins build directories and the local acornui plugins repo."
			internalPluginsBuilds.forEach { this.dependsOn(it.task(":${this.name}")) }
		}

		val wrapperConsumerIncludedBuilds by creating(Wrapper::class) {
			group = "build setup"
			description = "Helper to build the gradle wrappers for acornui and consumer included builds in case " +
					"things get stuck."
			allSansPluginsBuilds.forEach { this.dependsOn(it.task(":wrapper")) }
		}

		val wrapperPluginIncludedBuilds by creating(Wrapper::class) {
			group = "build setup"
			description = "Helper to build the gradle wrappers for acornui plugin included builds in case things " +
					"get stuck."
			internalPluginsBuilds.forEach { this.dependsOn(it.task(":wrapper")) }
		}

		val wrapperAllIncludedBuilds by creating(Wrapper::class) {
			group = "build setup"
			description = "Helper to build the gradle wrappers for acornui (including plugins) and consumer included " +
					"builds in case things get stuck."
			gradle.includedBuilds.forEach { this.dependsOn(it.task(":wrapper")) }
		}
	}
}

val GRADLE_VERSION by acornConfig
tasks.withType<Wrapper> {
	gradleVersion = GRADLE_VERSION
	distributionType = Wrapper.DistributionType.ALL
}

// Uncomment below to get access to adhoc debugging (also a good place ot try new things).
//throw Exception(this.extra.properties.entries.joinToString("\n") { "${it.key.padStart(40)}: ${it.value}" })
//apply(from = "$ACORNUI_SCRIPTS_PLUGINS_PATH/adhoc.debug.build.gradle.kts")
//val ACORNUI_SCRIPTS_PLUGINS_PATH: String by acornConfig

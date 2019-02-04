import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels

val acornUiHome: String = System.getenv()["ACORNUI_HOME"] ?: throw Exception("Environment variable ACORNUI_HOME must be set.")
if (!File(acornUiHome).exists()) throw Exception("ACORNUI_HOME '$acornUiHome' does not exist.")

val repo = "http://repo1.maven.org/maven2"
val lwjglVersion = "3.2.1"

val knownDependencies = mutableSetOf<String>()
val knownDependencyLocations = mutableSetOf<String>()

dependency("$repo/com/bladecoder/packr/packr/2.1/packr-2.1", "tools/acornui-build-tasks/src/jvmMain", includeSources = false, includeDocs = false)
dependency("$repo/org/zeroturnaround/zt-zip/1.10/zt-zip-1.10", "tools/acornui-build-tasks/src/jvmMain", includeSources = false, includeDocs = false)
dependency("$repo/com/eclipsesource/minimal-json/minimal-json/0.9.1/minimal-json-0.9.1", "tools/acornui-build-tasks/src/jvmMain", includeSources = false, includeDocs = false)
dependency("$repo/com/lexicalscope/jewelcli/jewelcli/0.8.9/jewelcli-0.8.9", "tools/acornui-build-tasks/src/jvmMain", includeSources = false, includeDocs = false)
dependency("$repo/org/apache/commons/commons-io/1.3.2/commons-io-1.3.2", "tools/acornui-build-tasks/src/jvmMain", includeSources = false, includeDocs = false)
dependency("$repo/org/slf4j/slf4j-simple/1.6.6/slf4j-simple-1.6.6", "tools/acornui-build-tasks/src/jvmMain", includeSources = false, includeDocs = false)
dependency("$repo/org/slf4j/slf4j-api/1.7.9/slf4j-api-1.7.9", "tools/acornui-build-tasks/src/jvmMain", includeSources = false, includeDocs = false)

dependency("$repo/com/google/code/gson/gson/2.7/gson-2.7", "tools/acornui-build-tasks/src/jvmMain")
dependency("$repo/com/google/guava/guava/20.0/guava-20.0", "tools/acornui-build-tasks/src/jvmMain")

val closureVersion = "v20190106" // old v20170626
dependency("$repo/com/google/javascript/closure-compiler-externs/$closureVersion/closure-compiler-externs-$closureVersion", "tools/acornui-build-tasks/src/jvmMain", includeSources = false, includeDocs = false)
dependency("$repo/com/google/javascript/closure-compiler/$closureVersion/closure-compiler-$closureVersion", "tools/acornui-build-tasks/src/jvmMain")


val natives = arrayOf("windows", "macos", "linux")
val extensions = arrayOf("glfw", "jemalloc", "opengl", "openal", "stb", "nfd")
for (native in natives) {
	runtimeDependency("$repo/org/lwjgl/lwjgl/$lwjglVersion/lwjgl-$lwjglVersion-natives-$native", "backends/acornui-lwjgl-backend/src/jvmMain")
	for (extension in extensions) {
		runtimeDependency("$repo/org/lwjgl/lwjgl-$extension/$lwjglVersion/lwjgl-$extension-$lwjglVersion-natives-$native", "backends/acornui-lwjgl-backend/src/jvmMain")
	}
}

dependency("$repo/org/lwjgl/lwjgl/$lwjglVersion/lwjgl-$lwjglVersion", "backends/acornui-lwjgl-backend/src/jvmMain")
for (extension in extensions) {
	dependency("$repo/org/lwjgl/lwjgl-$extension/$lwjglVersion/lwjgl-$extension-$lwjglVersion", "backends/acornui-lwjgl-backend/src/jvmMain")
}
dependency("$repo/org/jcraft/jorbis/0.0.17/jorbis-0.0.17", "backends/acornui-lwjgl-backend/src/jvmMain")
dependency("$repo/com/badlogicgames/jlayer/jlayer/1.0.2-gdx/jlayer-1.0.2-gdx", "backends/acornui-lwjgl-backend/src/jvmMain")

val junitVersion = "4.12"
testDependency("$repo/junit/junit/$junitVersion/junit-$junitVersion")
testDependency("$repo/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3")
testDependency("$repo/org/mockito/mockito-core/1.10.19/mockito-core-1.10.19")
testDependency("$repo/org/objenesis/objenesis/2.1/objenesis-2.1")

fun dependency(path: String, module: String, includeSources: Boolean = true, includeDocs: Boolean = true) {
	downloadJars(path, "$acornUiHome/$module/externalLib/compile/${path.substringAfterLast("/")}", includeSources, includeDocs)
}

fun runtimeDependency(path: String, module: String) {
	downloadJars(path, "$acornUiHome/$module/externalLib/runtime/${path.substringAfterLast("/")}", includeSources = false, includeDocs = false)
}

fun testDependency(path: String, includeSources: Boolean = true, includeDocs: Boolean = true) {
	downloadJars(path, "$acornUiHome/externalLib/test/${path.substringAfterLast("/")}", includeSources, includeDocs)
}

fun downloadJars(path: String, destination: String, includeSources: Boolean, includeDocs: Boolean) {
	download("$path.jar", "$destination.jar")
	if (includeSources) download("$path-sources.jar", "$destination-sources.jar")
	if (includeDocs) download("$path-javadoc.jar", "$destination-javadoc.jar")
}

fun download(path: String, destination: String) {
	val dest = File(destination)
	knownDependencies.add(dest.absolutePath)
	knownDependencyLocations.add(dest.parent!!)
	if (dest.exists()) return // Already up-to-date.
	dest.parentFile.mkdirs()
	val connection = URL(path).openConnection()
	val outStream = FileOutputStream(destination)
	val inChannel = Channels.newChannel(connection.inputStream)
	var position = 0L
	val contentLength = connection.contentLength
	println("Downloading $path to ${dest.absolutePath}")
	print(".")
	val bars = 100
	var currentBars = 1
	do {
		val transferred = outStream.channel.transferFrom(inChannel, position, 1024 * 32)
		position += transferred
		val desiredBars = bars * position.toFloat() / contentLength
		while (currentBars++ < desiredBars) {
			print(".")
		}
	} while (transferred > 0)
	println("")
}

// Cleanup dependencies
for (knownDependencyLocation in knownDependencyLocations) {
	for (listFile in File(knownDependencyLocation).listFiles()) {
		if (!knownDependencies.contains(listFile.absolutePath)) {
			println("Deleting old dependency: ${listFile.absolutePath}")
			listFile.delete()
		}
	}
}
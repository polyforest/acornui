import java.io.File

val acornUiHome: String = System.getenv()["ACORNUI_HOME"] ?: throw Exception("Environment variable ACORNUI_HOME must be set.")
if (!File(acornUiHome).exists()) throw Exception("ACORNUI_HOME '$acornUiHome' does not exist.")

for (file in File(acornUiHome).walkTopDown()) {
	if (file.name == "externalLib") {
		print("Deleting ${file.absolutePath}... ")
		val success = file.deleteRecursively()
		println(if (success) "success" else "failed")
	}
}

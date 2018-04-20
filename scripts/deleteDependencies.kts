import java.io.File

val ACORNUI_HOME: String = System.getenv()["ACORNUI_HOME"] ?: throw Exception("Environment variable ACORNUI_HOME must be set.")
if (!File(ACORNUI_HOME).exists()) throw Exception("ACORNUI_HOME '$ACORNUI_HOME' does not exist.")

for (file in File(ACORNUI_HOME).walkTopDown()) {
	if (file.name == "externalLib") {
		print("Deleting ${file.absolutePath}... ")
		val success = file.deleteRecursively()
		println(if (success) "success" else "failed")
	}
}

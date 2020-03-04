import com.acornui.runMain
import com.acornui.lwjgl.lwjglApplication

/**
 * `main` (with optional args: Array<String>) is our main entry point.
 *
 * This method is wrapped in a [runMain] block to set up the main loop and context.
 */
fun main() = runMain {

	// lwjglApplication is an acornui backend for the JVM.
	lwjglApplication(config) {
		// Create and add our main component to the stage:
		+mainComponent()
	}
}
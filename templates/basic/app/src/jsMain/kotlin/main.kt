import com.acornui.runMain
import com.acornui.webgl.webGlApplication

/**
 * `main` is our main entry point.
 *
 * This method is wrapped in a [runMain] block to set up the main loop and context.
 */
fun main() = runMain {

	// webGlApplication is an acornui backend for the browser.
	webGlApplication("acornUiRoot") {
		// Create and add our main component to the stage:
		+mainComponent()
	}
}
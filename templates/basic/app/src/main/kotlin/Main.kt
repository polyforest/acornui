import com.acornui.application
import com.acornui.component.DivComponent
import com.acornui.component.StageImpl
import com.acornui.component.input.button
import com.acornui.component.style.StyleTag
import com.acornui.di.Context
import com.acornui.dom.add
import com.acornui.dom.addCssToHead
import com.acornui.dom.head
import com.acornui.dom.linkElement
import com.acornui.input.clicked
import com.acornui.runMain
import com.acornui.skins.Theme
import com.acornui.skins.addCssToHead
import com.acornui.version

/**
 * An example of input controls.
 */
class Main(owner: Context) : DivComponent(owner) {

	init {
		println(version)

		Theme().addCssToHead()
		addClass(styleTag)

		head.add(
			linkElement(
				"https://fonts.googleapis.com/css2?family=Roboto+Mono:wght@200;400&display=swap",
				rel = "stylesheet"
			)
		)

		+button("Hello World") {
			clicked.listen {
				println("Hello World")
			}
		}
	}

	@Suppress("CssOverwrittenProperties")
	companion object {

		val styleTag = StyleTag("Main")

		init {
			addCssToHead(
				"""

$styleTag {
  font-family: 'Roboto Mono', monospace;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

"""
			)
		}
	}
}

/**
 * `main` is our main entry point.
 *
 * This method is wrapped in a [runMain] block to set up the main context.
 */
fun main() = runMain {

	application("acornUiRoot") {
		// Create and add our main component to the stage:
		+Main(this)
	}
}
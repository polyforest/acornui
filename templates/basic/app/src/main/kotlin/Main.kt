import com.acornui.app
import com.acornui.component.Div
import com.acornui.component.StageImpl
import com.acornui.component.input.button
import com.acornui.component.style.cssClass
import com.acornui.di.Context
import com.acornui.dom.add
import com.acornui.dom.addCssToHead
import com.acornui.dom.head
import com.acornui.dom.linkElement
import com.acornui.input.clicked
import com.acornui.skins.Theme
import com.acornui.skins.addCssToHead
import com.acornui.version

/**
 * A barebones example with a Theme and a Button.
 */
class Main(owner: Context) : Div(owner) {

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

		val styleTag by cssClass()

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
 */
fun main() = app("acornUiRoot") {
	// Create and add our main component to the stage:
	+Main(this)
}
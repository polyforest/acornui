import com.acornui.component.*
import com.acornui.component.layout.HAlign
import com.acornui.component.layout.VAlign
import com.acornui.di.Context
import com.acornui.input.interaction.click
import com.acornui.popup.PopUpInfo
import com.acornui.popup.addPopUp
import com.acornui.skins.BasicUiSkin
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * This is a Hello World view container.
 * For more examples and documentation, go to [https://github.com/polyforest/acornui/wiki]
 *
 * @param owner The context that created this component.
 */
class MainComponent(owner: Context) : StackLayoutContainer<UiComponent>(owner) {

	init {
		// Adds the basic skin to the stage. This sets up styling for all Acorn UI components.
		BasicUiSkin(stage).apply()

		style.horizontalAlign = HAlign.CENTER
		style.verticalAlign = VAlign.MIDDLE

		+button("Button 1") {
			click().add {
				println("Button 1 clicked")
				addPopUp(PopUpInfo(windowPanel {
					+image("assets/exampleAtlas.json", "Test1")
				}, isModal = true))
			}
		}
	}
}

/**
 * Constructs [MainComponent] and initializes with [init].
 */
inline fun Context.mainComponent(init: ComponentInit<MainComponent> = {}): MainComponent {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return MainComponent(this).apply(init)
}
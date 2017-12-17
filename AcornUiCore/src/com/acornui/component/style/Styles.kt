@file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

package com.acornui.component.style

import com.acornui._assert
import com.acornui.assertionsEnabled
import com.acornui.collection.ActiveList
import com.acornui.collection.addSorted
import com.acornui.collection.find2
import com.acornui.collection.removeFirst
import com.acornui.component.AttachmentHolder
import com.acornui.core.Disposable
import com.acornui.observe.Observable
import com.acornui.observe.bind

interface StyleableRo {

	/**
	 * The current [StyleTag] objects added. This list must be unique.
	 *
	 * The curated tags list will be passed to the style entry filter
	 */
	val styleTags: List<StyleTag>

	/**
	 * A list of style rules that will be queried in determining calculated values for bound style objects.
	 */
	val styleRules: List<StyleRule<*>>

	fun <T : StyleRo> getRulesByType(type: StyleType<T>, out: MutableList<StyleRule<T>>)

	/**
	 * The next ancestor of this styleable component.
	 */
	val styleParent: StyleableRo?

	fun invalidateStyles()
}

interface Styleable : StyleableRo {

	/**
	 * The current [StyleTag] objects added. This list must be unique.
	 *
	 * The curated tags list will be passed to the style entry filter
	 */
	override val styleTags: MutableList<StyleTag>

	/**
	 * A list of style rules that will be queried in determining calculated values for bound style objects.
	 */
	override val styleRules: MutableList<StyleRule<*>>

}

fun Styleable.addStyleRule(style: StyleRo, filter: StyleFilter, priority: Float = 0f) {
	styleRules.add(StyleRule(style, filter, priority))
}

fun Styleable.addStyleRule(style: StyleRo, priority: Float = 0f) {
	styleRules.add(StyleRule(style, AlwaysFilter, priority))
}

class StylesImpl(private val host: Styleable) : Disposable {

	val styleTags = ActiveList<StyleTag>()
	val styleRules = ActiveList<StyleRule<*>>()

	private val entriesByType = HashMap<StyleType<*>, MutableList<StyleRule<*>>>()

	private val styleValidators = ArrayList<StyleValidator>()
	private val styleWatchers = ArrayList<StyleWatcher<*>>()

	init {
		styleTags.bind(host::invalidateStyles)
		styleRules.added.add {
			index, entry ->
			add(entry)
		}
		styleRules.removed.add {
			index, entry ->
			remove(entry)
		}
		styleRules.changed.add {
			index, oldEntry, newEntry ->
			remove(oldEntry)
			add(newEntry)
		}
		styleRules.reset.add {
			entriesByType.clear()
			for (entry in styleRules) add(entry)
		}

	}

	private fun add(entry: StyleRule<*>) {
		if (!entriesByType.containsKey(entry.style.type))
			entriesByType[entry.style.type] = ArrayList()
		entriesByType[entry.style.type]!!.add(entry)
		host.invalidateStyles()
	}

	private fun remove(entry: StyleRule<*>) {
		entriesByType[entry.style.type]?.remove(entry)
		host.invalidateStyles()
	}

	fun <T : StyleRo> getRulesByType(type: StyleType<T>, out: MutableList<StyleRule<T>>) {
		@Suppress("UNCHECKED_CAST")
		val entries = entriesByType[type] as List<StyleRule<T>>?
		out.clear()
		if (entries != null)
			out.addAll(entries)
	}

	fun <T : Style> bind(style: T, calculator: StyleCalculator = CascadingStyleCalculator): T {
		style.changed.add(this::styleChangedHandler)
		styleValidators.add(StyleValidator(style, calculator))
		host.invalidateStyles()
		return style
	}

	fun unbind(style: StyleRo) {
		style.changed.remove(this::styleChangedHandler)
		styleValidators.removeFirst { it.style === style }
	}

	fun <T : Style> watch(style: T, priority: Float, callback: (T) -> Unit) {
		if (assertionsEnabled)
			_assert(styleValidators.find2 { it.style === style } != null, "A style object is being watched without being bound. Use `val yourStyle = bind(YourStyle())`.")
		val watcher = StyleWatcher(style, priority, callback)
		styleWatchers.addSorted(watcher)
	}

	fun unwatch(style: Style) {
		styleWatchers.removeFirst { it.style === style }
	}

	fun validateStyles() {
		for (i in 0..styleValidators.lastIndex) {
			styleValidators[i].validate(host)
		}
		for (i in 0..styleWatchers.lastIndex) {
			styleWatchers[i].check()
		}
	}

	fun styleChangedHandler(o: Observable) {
		host.invalidateStyles()
	}

	override fun dispose() {
		for (i in 0..styleValidators.lastIndex) {
			styleValidators[i].style.changed.remove(this::styleChangedHandler)
		}
		styleValidators.clear()
		styleTags.dispose()
		styleRules.dispose()
	}
}


/**
 * Holds a style object with explicit values that will be applied to the calculated values of the target style object
 * when the filter passes.
 * Entries will be applied in the order from deepest child to highest ancestor (the stage).
 */
class StyleRule<out T : StyleRo>(

		/**
		 * The explicit values on this style object will be applied to the target if the filter passes.
		 * If this style object changes, the styles object will dispatch a changed signal.
		 */
		val style: T,

		/**
		 * A filter responsible for determining whether or not this rule should be applied.
		 */
		val filter: StyleFilter,

		/**
		 * A higher priority value will be applied before entries with a lower priority.
		 * Equivalent priorities will go to the entry deeper in the display hierarchy, and then to the order this entry
		 * was added.
		 */
		val priority: Float = 0f
)

interface StyleType<out T : StyleRo> {

	val extends: StyleType<*>?
		get() = null
}

inline fun StyleType<*>.walkInheritance(callback: (StyleType<*>) -> Unit) {
	var e: StyleType<*>? = this
	while (e != null) {
		callback(e)
		e = e.extends
	}
}

/**
 * Style tags are markers placed on the display hierarchy that are used for filtering which styles from [StyleRule]
 * objects are applied.
 *
 * A StyleTag may be used as a [StyleFilter], with the default implementation passing the filter if the current target
 * contains this tag.
 */
interface StyleTag : StyleFilter {
	override fun invoke(target: StyleableRo): StyleableRo? {
		if (target.styleTags.contains(this)) return target
		return null
	}
}

/**
 * Constructs a new StyleTag object.
 */
fun styleTag(): StyleTag = object : StyleTag {}

inline fun StyleableRo.walkStyleableAncestry(callback: (StyleableRo) -> Unit) {
	var p: StyleableRo? = this
	while (p != null) {
		callback(p)
		p = p.styleParent
	}
}
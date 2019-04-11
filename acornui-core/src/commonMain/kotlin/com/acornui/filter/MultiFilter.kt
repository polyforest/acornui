package com.acornui.filter

import com.acornui.core.Renderable
import com.acornui.core.di.Owned
import com.acornui.math.MinMaxRo


/**
 * A multi filter is a list of filters to be applied in the order they were added.
 */
class MultiFilter(owner: Owned) : RenderFilterBase(owner) {

	private val _filters = RenderFilterList(null)
	val filters: MutableList<RenderFilter> = _filters

	override var contents: Renderable? = null
		set(value) {
			field = value
			_filters.tail = value
		}

	operator fun <T : RenderFilter> T.unaryPlus(): T {
		filters.add(this)
		return this
	}

	operator fun <T : RenderFilter> T.unaryMinus(): T {
		filters.remove(this)
		return this
	}

	override val shouldSkipFilter: Boolean
		get() = super.shouldSkipFilter || filters.isEmpty()

	override fun draw(clip: MinMaxRo) {
		filters.first().render(clip)
	}
}

fun Owned.multiFilter(vararg renderFilters: RenderFilter): MultiFilter {
	val m = MultiFilter(this)
	m.filters.addAll(renderFilters)
	return m
}
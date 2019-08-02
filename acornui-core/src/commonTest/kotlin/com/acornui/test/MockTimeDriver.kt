package com.acornui.test

import com.acornui.ChildRo
import com.acornui.ParentRo
import com.acornui.TimeDriverConfig
import com.acornui.UpdatableChild
import com.acornui.time.TimeDriver

object MockTimeDriver : TimeDriver {

	override val config: TimeDriverConfig = TimeDriverConfig(0f, 0)

	override fun activate() {
	}

	override fun update() {
	}

	override val parent: ParentRo<ChildRo>? = null
	override val children: List<UpdatableChild> = emptyList()

	override fun <S : UpdatableChild> addChild(index: Int, child: S): S {
		return child
	}

	override fun removeChild(index: Int): UpdatableChild = MockUpdatableChild
}
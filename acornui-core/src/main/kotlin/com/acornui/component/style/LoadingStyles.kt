/*
 * Copyright 2020 Poly Forest, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.acornui.component.style

import com.acornui.component.Stage
import com.acornui.component.UiComponent
import com.acornui.component.stage
import com.acornui.component.style.LoadingStyles.loading
import com.acornui.di.Context
import com.acornui.di.dependencyFactory
import com.acornui.dom.addStyleToHead
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

object LoadingStyles {

	val loading by cssClass()

	val showOnLoading by cssClass()

	init {
		addStyleToHead(
			"""
$showOnLoading {
	opacity: 0;
	transition: opacity ease 0.5s;
}
			
$loading $showOnLoading {
	opacity: 1;
}
		"""
		)
	}
}

/**
 * Runs the suspend block wrapped in with [loadingCount] increment/decrement.
 * This will show elements with the [LoadingStyles.showOnLoading] class.
 */
fun UiComponent.launchWithIndicator(
	context: CoroutineContext = EmptyCoroutineContext,
	start: CoroutineStart = CoroutineStart.DEFAULT,
	block: suspend CoroutineScope.() -> Unit
) {
	launch(context, start) {
		loadingCount++
		block()
		loadingCount--
	}
}

/**
 * Tracks the number of things loading.
 * If the loading counter changes from 0, [LoadingStyles.loading] class is added to [Stage].
 * If the counter changes to 0 [LoadingStyles.loading] class is removed from [Stage].
 */
class LoadingCounter(private val stage: Stage) {

	var count = 0
		set(value) {
			val previous = field
			if (previous == value) return
			field = value
			if (value == 0) {
				stage.removeClass(loading)
			} else if (previous == 0) {
				stage.addClass(loading)
			}
		}

	companion object : Context.Key<LoadingCounter> {
		override val factory = dependencyFactory { LoadingCounter(it.stage) }
	}
}
private var loadingCount = 0
private var Context.loadingCount: Int
	get() = inject(LoadingCounter).count
	set(value) {
		inject(LoadingCounter).count = value
	}

private fun Context.showLoading(): () -> Unit {
	var hasFinished = false
	loadingCount++
	return {
		if (!hasFinished) {
			hasFinished = true
			loadingCount--
		}
	}
}
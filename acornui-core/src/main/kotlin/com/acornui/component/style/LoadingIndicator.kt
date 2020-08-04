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

import com.acornui.component.AttachmentHolder
import com.acornui.component.UiComponent
import com.acornui.component.style.LoadingIndicator.loading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

object LoadingIndicator {

	val loading = StyleTag("loading")

	val showOnLoading = StyleTag("showOnLoading")
}

fun UiComponent.launchWithIndicator(
	context: CoroutineContext = EmptyCoroutineContext,
	start: CoroutineStart = CoroutineStart.DEFAULT,
	block: suspend CoroutineScope.() -> Unit
) {
	launch(context, start) {

		block()
	}
}

private var UiComponent.loadingCount: Int
	get() = getAttachment("loadingCount") ?: 0
	set(value) {
		setAttachment("loadingCount", value)
		if (value == 0) {
			removeClass(loading)
		} else if (value == 1) {
			addClass(loading)
		}
	}
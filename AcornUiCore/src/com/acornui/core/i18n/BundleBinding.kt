/*
 * Copyright 2017 Nicholas Bilyk
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

package com.acornui.core.i18n

import com.acornui.core.Disposable
import com.acornui.core.di.Injector
import com.acornui.core.di.Scoped
import com.acornui.signal.SignalHandlerSet

/**
 * A `BundleBinding` object loads an [I18nBundle] object, then allows changed handlers to be added as a set, which
 * are then removed upon [dispose].
 */
class BundleBinding(override val injector: Injector, bundleName: String) : Scoped, Disposable {

	private val signalHandlerSet: SignalHandlerSet<(I18nBundle) -> Unit>
	private val bundle: I18nBundle

	init {
		bundle = loadBundle(bundleName)
		signalHandlerSet = SignalHandlerSet(bundle.changed)
	}

	operator fun invoke(callback: (I18nBundle) -> Unit) {
		signalHandlerSet.add(callback)
		callback(bundle)
	}

	override fun dispose() {
		signalHandlerSet.dispose()
	}
}


/**
 * Invokes the callback when this bundle has changed.
 */
fun Scoped.i18n(bundleName: String) : BundleBinding {
	return BundleBinding(injector, bundleName)
}
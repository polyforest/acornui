/*
 * Copyright 2019 Poly Forest, LLC
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

package com.acornui.i18n

import com.acornui.Disposable
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import com.acornui.observe.Observable
import com.acornui.signal.Signal1
import com.acornui.toDisposable

/**
 * This class is responsible for tracking a set of callbacks and cached files for an [I18n] bundle.
 * When this binding is disposed, the handlers are all removed and the cached file references are decremented.
 */
class BundleBinding(owner: Context, bundleName: String) : ContextImpl(owner), I18nBundleRo {

	private val _changed = Signal1<I18nBundleRo>()
	override val changed = _changed.asRo()


	/**
	 * The bundle this binding is watching.
	 */
	private val bundle: I18nBundleRo = inject(I18n).getBundle(bundleName)

	init {
		bundle.changed.add(::bundleChangedHandler)
	}

	fun bind(callback: (bundle: I18nBundleRo) -> Unit): Disposable {
		_changed.add(callback)
		callback(bundle)
		return {
			_changed.remove(callback)
		}.toDisposable()
	}

	private fun bundleChangedHandler(o: Observable) {
		_changed.dispatch(this)
	}

	override fun get(key: String): String? = bundle[key]

	override fun dispose() {
		super.dispose()
		bundle.changed.remove(::bundleChangedHandler)
		_changed.dispose()
	}
}

/**
 * Invokes the callback when this bundle has changed.
 */
fun Context.i18n(bundleName: String) : BundleBinding {
	return BundleBinding(this, bundleName)
}

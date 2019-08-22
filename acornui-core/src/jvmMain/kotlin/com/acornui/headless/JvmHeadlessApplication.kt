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

@file:Suppress("unused")

package com.acornui.headless

import com.acornui.AppConfig
import com.acornui.ApplicationBase
import com.acornui.JvmApplicationRunner
import com.acornui.asset.Loaders
import com.acornui.async.uiThread
import com.acornui.di.Injector
import com.acornui.di.InjectorImpl
import com.acornui.di.Owned
import com.acornui.di.OwnedImpl
import com.acornui.graphic.RgbData
import com.acornui.io.*
import com.acornui.io.file.Files
import com.acornui.io.file.FilesImpl
import com.acornui.io.file.ManifestUtil
import com.acornui.mock.MockInjector
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * A Headless application initializes utility dependencies, but does not create any windowing, graphics, or input.
 * @author nbilyk
 */
open class JvmHeadlessApplication(
		private val assetsPath: String = "./",
		private val assetsRoot: String = "./"
) : ApplicationBase() {

	init {
		uiThread = Thread.currentThread()
	}

	override fun start(appConfig: AppConfig, onReady: Owned.() -> Unit) {
		set(AppConfig, appConfig)
		val injector = runBlocking { createInjector() }
		val owner = OwnedImpl(injector)
		owner.onReady()
		JvmApplicationRunner(owner.injector).run()
		owner.dispose()
		dispose()
	}

	override suspend fun createInjector(): Injector = InjectorImpl(MockInjector.create(), bootstrap.dependenciesList())

	override val filesTask by task(Files) {
		val manifest = ManifestUtil.createManifest(File(assetsPath), File(assetsRoot))
		FilesImpl(manifest)
	}

	protected open val rgbDataLoader by task(Loaders.rgbDataLoader) {
		object : Loader<RgbData> {
			override val defaultInitialTimeEstimate: Float
				get() = Bandwidth.downBpsInv * 100_000

			override suspend fun load(requestData: UrlRequestData, progressReporter: ProgressReporter, initialTimeEstimate: Float): RgbData {
				return loadRgbData(requestData, progressReporter, initialTimeEstimate)
			}
		}
	}

}



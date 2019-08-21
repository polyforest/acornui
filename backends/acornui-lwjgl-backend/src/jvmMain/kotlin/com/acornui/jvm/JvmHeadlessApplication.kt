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

package com.acornui.jvm

import com.acornui.AppConfig
import com.acornui.ApplicationBase
import com.acornui.asset.Loaders
import com.acornui.async.globalLaunch
import com.acornui.async.uiThread
import com.acornui.di.OwnedImpl
import com.acornui.di.Scoped
import com.acornui.graphic.RgbData
import com.acornui.io.Bandwidth
import com.acornui.io.Loader
import com.acornui.io.ProgressReporter
import com.acornui.io.UrlRequestData
import com.acornui.io.file.Files
import com.acornui.io.file.FilesImpl
import com.acornui.io.file.ManifestUtil
import com.acornui.jvm.opengl.loadRgbData
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

	fun start(appConfig: AppConfig = AppConfig(),
			  onReady: Scoped.() -> Unit = {}) {
		set(AppConfig, appConfig)
		globalLaunch {
			awaitAll()
			val injector = createInjector()
			OwnedImpl(injector).onReady()
		}
	}

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

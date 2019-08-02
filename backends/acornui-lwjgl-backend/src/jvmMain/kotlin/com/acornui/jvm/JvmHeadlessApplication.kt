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

import com.acornui.async.Promise
import com.acornui.async.launch
import com.acornui.AppConfig
import com.acornui.ApplicationBase
import com.acornui.asset.AssetManager
import com.acornui.asset.AssetManagerImpl
import com.acornui.asset.AssetType
import com.acornui.asset.LoaderFactory
import com.acornui.di.OwnedImpl
import com.acornui.di.Scoped
import com.acornui.io.file.Files
import com.acornui.io.file.FilesImpl
import com.acornui.io.file.ManifestUtil
import com.acornui.jvm.graphic.JvmRgbDataLoader
import com.acornui.jvm.loader.JvmTextLoader
import com.acornui.jvm.loader.WorkScheduler
import java.io.File

/**
 * A Headless application initializes utility dependencies, but does not create any windowing, graphics, or input.
 * @author nbilyk
 */
open class JvmHeadlessApplication(
		private val assetsPath: String = "./",
		private val assetsRoot: String = "./"
) : ApplicationBase() {

	fun start(appConfig: AppConfig = AppConfig(),
			  onReady: Scoped.() -> Unit = {}) {
		set(AppConfig, appConfig)
		launch {
			awaitAll()
			val injector = createInjector()
			OwnedImpl(injector).onReady()
		}
	}

	override val filesTask by task(Files) {
		val manifest = ManifestUtil.createManifest(File(assetsPath), File(assetsRoot))
		FilesImpl(manifest)
	}

	private fun <T> ioWorkScheduler(): WorkScheduler<T> = { work ->
		object : Promise<T>() {
			init {
				try {
					success(work())
				} catch (e: Throwable) {
					fail(e)
				}
			}
		}
	}

	override val assetManagerTask by task(AssetManager) {
		val loaders = HashMap<AssetType<*>, LoaderFactory<*>>()
		loaders[AssetType.TEXT] = { path, _ -> JvmTextLoader(path, Charsets.UTF_8) }
		loaders[AssetType.RGB_DATA] = { path, _ -> JvmRgbDataLoader(path) }
		AssetManagerImpl("", get(Files), loaders)
	}
}

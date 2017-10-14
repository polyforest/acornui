/*
 * Copyright 2015 Nicholas Bilyk
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

package com.acornui.jvm

import com.acornui.assertionsEnabled
import com.acornui.browser.decodeUriComponent2
import com.acornui.browser.encodeUriComponent2
import com.acornui.core.AppConfig
import com.acornui.core.assets.AssetManager
import com.acornui.core.assets.AssetManagerImpl
import com.acornui.core.assets.AssetTypes
import com.acornui.core.di.Injector
import com.acornui.core.di.InjectorImpl
import com.acornui.core.di.Scoped
import com.acornui.core.io.JSON_KEY
import com.acornui.core.io.file.Files
import com.acornui.core.io.file.FilesImpl
import com.acornui.core.lineSeparator
import com.acornui.core.text.NumberFormatter
import com.acornui.core.text.DateTimeFormatter
import com.acornui.core.time.time
import com.acornui.jvm.graphics.JvmRgbDataLoader
import com.acornui.jvm.io.file.ManifestUtil
import com.acornui.jvm.loader.JvmTextLoader
import com.acornui.jvm.text.NumberFormatterImpl
import com.acornui.jvm.text.DateTimeFormatterImpl
import com.acornui.jvm.time.TimeProviderImpl
import com.acornui.logging.ILogger
import com.acornui.logging.Log
import com.acornui.serialization.JsonSerializer
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * A Headless application initializes utility dependencies, but does not create any windowing, graphics, or input.
 * @author nbilyk
 */
open class JvmHeadlessApplication(
		private val assetsPath: String = "./",
		private val assetsRoot: String = "./",
		private val config: AppConfig = AppConfig(),
		onReady: Scoped.() -> Unit = {}
) : Scoped {

	protected val _injector = InjectorImpl()
	override val injector: Injector
		get() = _injector

	init {
		initializeBootstrap()
		_injector.lock()
		onReady()
	}

	protected open fun initializeBootstrap() {
		_injector[AppConfig] = config
		initializeDebug()
		initializeSystem()
		initializeLogging()
		initializeNumber()
		initializeString()
		initializeTime()
		initializeJson()
		initializeFiles()
		initializeAssetManager()
	}

	protected open fun initializeDebug() {
		config.debug = config.debug || System.getProperty("debug")?.toLowerCase() == "true"
		if (config.debug) {
			assertionsEnabled = true
		}
	}

	protected open fun initializeSystem() {
		lineSeparator = System.lineSeparator()
	}

	protected open fun initializeLogging() {
		if (config.debug) {
			Log.level = ILogger.DEBUG
		} else {
			Log.level = ILogger.INFO
		}
	}

	/**
	 * Initializes number constants and methods
	 */
	protected open fun initializeNumber() {
		_injector[NumberFormatter.FACTORY_KEY] = { NumberFormatterImpl(it) }
	}

	protected open fun initializeString() {
		encodeUriComponent2 = {
			str ->
			URLEncoder.encode(str, "UTF-8")
		}
		decodeUriComponent2 = {
			str ->
			URLDecoder.decode(str, "UTF-8")
		}
	}

	protected open fun initializeTime() {
		time = TimeProviderImpl()
		_injector[DateTimeFormatter.FACTORY_KEY] = { DateTimeFormatterImpl(it) }
	}

	protected open fun initializeJson() {
		_injector[JSON_KEY] = JsonSerializer
	}

	protected open fun initializeFiles() {
		val manifest = ManifestUtil.createManifest(File(assetsPath), File(assetsRoot))
		_injector[Files] = FilesImpl(manifest)
	}

	protected open fun initializeAssetManager() {
		val assetManager = AssetManagerImpl("", _injector.inject(Files))
		val isAsync = false
		assetManager.setLoaderFactory(AssetTypes.TEXT, { path, estimatedBytesTotal ->  JvmTextLoader(path, Charsets.UTF_8, isAsync) })
		assetManager.setLoaderFactory(AssetTypes.RGB_DATA, { path, estimatedBytesTotal -> JvmRgbDataLoader(path, isAsync) })
		_injector[AssetManager] = assetManager
	}
}
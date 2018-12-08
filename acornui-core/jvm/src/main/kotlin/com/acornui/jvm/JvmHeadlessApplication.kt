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
import com.acornui.async.Promise
import com.acornui.async.launch
import com.acornui.core.AppConfig
import com.acornui.core.ApplicationBase
import com.acornui.core.UserInfo
import com.acornui.core.asset.AssetManager
import com.acornui.core.asset.AssetManagerImpl
import com.acornui.core.asset.AssetType
import com.acornui.core.asset.LoaderFactory
import com.acornui.core.di.OwnedImpl
import com.acornui.core.di.Scoped
import com.acornui.core.i18n.I18n
import com.acornui.core.i18n.I18nImpl
import com.acornui.core.i18n.Locale
import com.acornui.core.io.file.Files
import com.acornui.core.io.file.FilesImpl
import com.acornui.core.text.dateTimeFormatterProvider
import com.acornui.core.text.numberFormatterProvider
import com.acornui.core.userInfo
import com.acornui.jvm.graphic.JvmRgbDataLoader
import com.acornui.jvm.io.file.ManifestUtil
import com.acornui.jvm.loader.JvmTextLoader
import com.acornui.jvm.loader.WorkScheduler
import com.acornui.jvm.text.DateTimeFormatterImpl
import com.acornui.jvm.text.NumberFormatterImpl
import com.acornui.logging.ILogger
import com.acornui.logging.Log
import java.io.File

/**
 * A Headless application initializes utility dependencies, but does not create any windowing, graphics, or input.
 * @author nbilyk
 */
open class JvmHeadlessApplication(
		private val assetsPath: String = "./",
		private val assetsRoot: String = "./"
) : ApplicationBase() {

	fun start(config: AppConfig = AppConfig(),
			  onReady: Scoped.() -> Unit = {}) {
		launch {
			initializeConfig(config)
			awaitAll()
			val injector = createInjector()
			OwnedImpl(injector).onReady()
		}
	}

	protected open fun initializeConfig(config: AppConfig) {
		val finalConfig = config.copy(debug = config.debug || System.getProperty("debug")?.toLowerCase() == "true")
		if (finalConfig.debug) {
			assertionsEnabled = true
		}
		if (finalConfig.debug) {
			Log.level = ILogger.DEBUG
		} else {
			Log.level = ILogger.INFO
		}
		set(AppConfig, finalConfig)
	}

	/**
	 * Sets the UserInfo dependency.
	 */
	protected open val userInfoTask by BootTask {
		val u = UserInfo(
				isTouchDevice = false,
				userAgent = "headless",
				platformStr = System.getProperty("os.name") ?: UserInfo.UNKNOWN_PLATFORM,
				systemLocale = listOf(Locale(java.util.Locale.getDefault().toLanguageTag()))
		)
		userInfo = u
		set(UserInfo, u)
	}

	protected open val i18nTask by BootTask {
		get(UserInfo)
		set(I18n, I18nImpl())
	}

	/**
	 * Initializes number constants and methods
	 */
	protected open val formatterTask by BootTask {
		get(UserInfo)
		numberFormatterProvider = { NumberFormatterImpl() }
		dateTimeFormatterProvider = { DateTimeFormatterImpl() }
	}

	protected open val filesTask by BootTask {
		val manifest = ManifestUtil.createManifest(File(assetsPath), File(assetsRoot))
		set(Files, FilesImpl(manifest))
	}

	private fun <T> ioWorkScheduler(): WorkScheduler<T> = {
		object : Promise<T>() {
			init {
				try {
					success(it())
				} catch (e: Throwable) {
					fail(e)
				}
			}
		}
	}

	protected open val assetManager by BootTask {
		val loaders = HashMap<AssetType<*>, LoaderFactory<*>>()
		loaders[AssetType.TEXT] = { path, _ ->  JvmTextLoader(path, Charsets.UTF_8, ioWorkScheduler()) }
		loaders[AssetType.RGB_DATA] = { path, _ -> JvmRgbDataLoader(path, ioWorkScheduler()) }
		val assetManager = AssetManagerImpl("", get(Files), loaders)
		set(AssetManager, assetManager)
	}
}
package com.acornui.headless

import com.acornui.AppConfig
import com.acornui.di.Owned

actual suspend fun headlessApplication(appConfig: AppConfig, onReady: Owned.() -> Unit) {
	return JvmHeadlessApplication().start(appConfig, onReady)
}
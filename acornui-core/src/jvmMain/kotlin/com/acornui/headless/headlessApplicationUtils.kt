package com.acornui.headless

import com.acornui.AppConfig
import com.acornui.component.Stage

actual suspend fun headlessApplication(appConfig: AppConfig, onReady: Stage.() -> Unit) {
	return JvmHeadlessApplication().start(appConfig, onReady)
}
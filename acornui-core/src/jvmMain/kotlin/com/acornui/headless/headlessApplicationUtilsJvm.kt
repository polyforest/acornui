package com.acornui.headless

import com.acornui.AppConfig
import com.acornui.MainContext
import com.acornui.component.Stage
import kotlinx.coroutines.Job

actual fun MainContext.headlessApplication(appConfig: AppConfig, onReady: Stage.() -> Unit): Job {
	return JvmHeadlessApplication(this).startAsync(appConfig, onReady)
}
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

package com.acornui

import com.acornui.audio.AudioManager
import com.acornui.audio.AudioManagerImpl
import com.acornui.component.Stage
import com.acornui.di.Context
import com.acornui.input.interaction.ContextMenuManager
import com.acornui.input.interaction.UndoDispatcher
import com.acornui.logging.Log
import com.acornui.persistence.JsPersistence
import com.acornui.persistence.Persistence
import com.acornui.system.userInfo
import org.w3c.dom.DocumentReadyState
import org.w3c.dom.LOADING
import kotlin.browser.document
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * The application base that would be used by all JS-based applications, including node-js.
 */
@Suppress("unused")
abstract class JsApplicationBase(mainContext: MainContext) : ApplicationBase(mainContext) {

	override suspend fun onBeforeStart() {
		contentLoad()
	}

	override suspend fun onStageCreated(stage: Stage) {
		super.onStageCreated(stage)
		initializeSpecialInteractivity(stage)
	}

	private suspend fun contentLoad() = suspendCoroutine<Unit> { cont ->
		if (userInfo.isBrowser && document.readyState == DocumentReadyState.LOADING) {
			document.addEventListener("DOMContentLoaded", {
				cont.resume(Unit)
			})
		} else {
			cont.resume(Unit)
		}
	}

	protected open val audioManagerTask by task(AudioManager) {
		// JS Audio doesn't need to be updated like OpenAL audio does, so we don't add it to the TimeDriver.
		AudioManagerImpl()
	}

	protected open val persistenceTask by task(Persistence) {
		JsPersistence(get(Version))
	}

	// TODO: Browserless clipboard

	protected open suspend fun initializeSpecialInteractivity(owner: Context) {
		UndoDispatcher(owner)
		ContextMenuManager(owner)
	}

	companion object {
		init {
			if (::memberRefTest != ::memberRefTest)
				Log.error("[SEVERE] Member reference equality fix isn't working.")

//			if (!userInfo.isBrowser && jsTypeOf(XMLHttpRequest) == "undefined") {
//				println("Requiring XMLHttpRequest")
////			js("""global.XMLHttpRequest = require("xmlhttprequest").XMLHttpRequest;""")
//			}
		}

		private fun memberRefTest() {}
	}
}
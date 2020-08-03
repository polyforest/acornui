/*
 * Copyright 2020 Poly Forest, LLC
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

@file:Suppress("MemberVisibilityCanBePrivate", "unused", "CssUnusedSymbol")

package com.acornui

import com.acornui.component.Stage
import com.acornui.component.stage
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import com.acornui.di.ContextMarker
import com.acornui.di.dependencyMapOf
import kotlinx.browser.document
import kotlinx.dom.clear
import org.w3c.dom.HTMLElement

/**
 * Creates an application under the receiver main context.
 *
 * @param rootId The root element id whose elements will be replaced with the new application's stage.
 */
fun MainContext.multiApp(
	rootId: String,
	appConfig: AppConfig = AppConfig(),
	init: Stage.() -> Unit
): Stage {
	val rootElement = document.getElementById(rootId).unsafeCast<HTMLElement?>()
		?: throw Exception("The root element with id $rootId could not be found.")
	return appContext(appConfig).stage().apply {
		rootElement.clear()
		rootElement.append(dom)
		init()
	}
}

/**
 * Creates an application under the receiver main context.
 *
 * The new application's stage will be appended to the document body.
 */
fun MainContext.multiApp(
	appConfig: AppConfig = AppConfig(),
	init: Stage.() -> Unit
): Stage = appContext(appConfig).stage().apply {
		document.body?.append(dom)
		init()
	}

/**
 * Creates a single application.
 *
  @param rootId The root element id whose elements will be replaced with the new application's stage.
 *
 * Dependencies of this application may not be shared with the dependencies of any other application, to create
 * multiple applications with shared dependencies, use [multiApp].
 */
fun app(
	rootId: String,
	appConfig: AppConfig = AppConfig(),
	init: Stage.() -> Unit
) {
	MainContext().multiApp(rootId, appConfig, init)
}

/**
 * Creates a single application, appending the new application's stage to the document body.
 *
 * Dependencies of this application may not be shared with the dependencies of any other application, to create
 * multiple applications with shared dependencies, use [multiApp].
 */
fun app(
	appConfig: AppConfig = AppConfig(),
	init: Stage.() -> Unit
) {
	MainContext().multiApp(appConfig, init)
}

private fun MainContext.appContext(appConfig: AppConfig) : Context = ContextImpl(owner = this, dependencies = dependencyMapOf(AppConfig to appConfig), marker = ContextMarker.APPLICATION)
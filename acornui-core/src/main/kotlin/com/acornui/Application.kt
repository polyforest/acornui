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
import com.acornui.di.*
import kotlinx.browser.document
import kotlinx.dom.clear
import org.w3c.dom.HTMLElement
import org.w3c.dom.ParentNode

/**
 * Creates an Acorn application.
 *
 * @param rootId The root element id whose elements will be replaced with the new application's stage.
 */
fun app(
	rootId: String,
	appConfig: AppConfig = AppConfig(),
	dependencies: DependencyMap = emptyDependencies,
	init: Stage.() -> Unit
) {
	val rootElement = document.getElementById(rootId).unsafeCast<HTMLElement?>()
		?: throw Exception("The root element with id $rootId could not be found.")
	rootElement.clear()
	app(appConfig, rootElement, dependencies, init)
}

/**
 * Creates an Acorn application, appending the new application's stage to the given [parentNode].
 *
 * @param appConfig
 * @param parentNode The parent node on which to append the stage. May be null.
 * @param init The initialization block with the [Stage] as the receiver.
 */
fun app(
	appConfig: AppConfig = AppConfig(),
	parentNode: ParentNode? = document.body,
	dependencies: DependencyMap = emptyDependencies,
	init: Stage.() -> Unit
) {
	appContext(appConfig, dependencies).apply {
		parentNode?.append(stage.dom)
		stage.init()
	}
}

private fun appContext(appConfig: AppConfig, dependencies: DependencyMap) : Context = ContextImpl(owner = mainContext, dependencies = dependencyMapOf(AppConfig to appConfig) + dependencies, marker = ContextMarker.APPLICATION)
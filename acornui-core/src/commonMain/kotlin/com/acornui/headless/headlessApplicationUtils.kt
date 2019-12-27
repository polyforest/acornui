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

@file:JvmName("HeadlessApplicationUtils")

package com.acornui.headless

import com.acornui.AppConfig
import com.acornui.component.Stage
import kotlin.jvm.JvmName

expect suspend fun headlessApplication(appConfig: AppConfig, onReady: Stage.() -> Unit)
suspend fun headlessApplication(onReady: Stage.() -> Unit) = headlessApplication(AppConfig(assetsManifestPath = null), onReady)
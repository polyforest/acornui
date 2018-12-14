/*
 * Copyright 2018 Poly Forest, LLC
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

rootProject.name = "acornui"

val ACORNUI_HOME = rootDir.canonicalPath
apply(from = "$ACORNUI_HOME/pathBootstrap.settings.gradle.kts")

val acornConfig: MutableMap<String, String> = gradle.startParameter.projectProperties
acornConfig["ACORNUI_HOME"] = ACORNUI_HOME
acornConfig["POLYFOREST_PROJECT"] = "true"
acornConfig["MULTI_MODULES"] = "acornui-core,tools/test-utils,tools/acornui-utils,tools/acornui-texturepacker"
acornConfig["MODULES"] = "acornui-game,acornui-spine,tools/acornui-buildutils"

val ACORNUI_SHARED_SETTINGS_PATH: String by gradle.startParameter.projectProperties
apply(from = "$ACORNUI_HOME/$ACORNUI_SHARED_SETTINGS_PATH")

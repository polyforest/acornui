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

package com.acornui.system

import com.acornui.i18n.Locale

/**
 * A singleton reference to the user info. This does not need to be scoped; there can only be one machine.
 */
actual val userInfo: UserInfo = UserInfo(
		isJvm = true,
		isJs = false,
		isBrowser = false,
		isMobile = false,
		userAgent = "jvm",
		platformStr = System.getProperty("os.name") ?: UserInfo.UNKNOWN_PLATFORM,
		systemLocale = listOf(Locale(java.util.Locale.getDefault().toLanguageTag()))
)
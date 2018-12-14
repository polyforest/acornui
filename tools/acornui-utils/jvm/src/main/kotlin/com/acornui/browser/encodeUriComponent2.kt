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

package com.acornui.browser

import java.net.URLDecoder
import java.net.URLEncoder

actual val encodeUriComponent2: (str: String) -> String  = { str ->
	URLEncoder.encode(str, "UTF-8")
}

actual val decodeUriComponent2: (str: String) -> String  = { str ->
	URLDecoder.decode(str, "UTF-8")
}
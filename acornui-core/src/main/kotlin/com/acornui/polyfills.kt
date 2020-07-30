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

package com.acornui

fun polyfills() {
	//language=JavaScript
	js(
		"""
		
// IE and edge no longer supported
//		if (typeof(Promise) == "undefined") { window.Promise = require('promise-polyfill').default; }
//		if (typeof(ResizeObserver) == "undefined") { window.ResizeObserver = require('resize-observer-polyfill').default; }

		if (typeof(Element) != "undefined" && typeof(Element.prototype.remove) == "undefined") {
			Element.prototype.remove = function() {
				this.parentNode.removeChild(this);
			}
		}	
"""
	)

}
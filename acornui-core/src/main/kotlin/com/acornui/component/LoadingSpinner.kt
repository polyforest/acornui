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

@file:Suppress("CssUnresolvedCustomProperty")

package com.acornui.component

 import com.acornui.component.style.cssClass
 import com.acornui.css.css
 import com.acornui.di.Context
 import com.acornui.dom.addStyleToHead
 import com.acornui.dom.div
 import com.acornui.skins.CssProps
 import kotlin.contracts.InvocationKind
 import kotlin.contracts.contract

/**
 *
 */
class LoadingSpinner(owner: Context) : Div(owner) {

	var ballCount: Int = 0
		set(value) {
			field = value
			clearElements(dispose = true)
			for (i in 0 until value) {
				+div {
					+" "
					applyCss("--p: ${(i + 1).toFloat() / value.toFloat()};")
				}
			}
		}

	init {
		addClass(LoadingSpinnerStyle.loadingSpinner)
		ballCount = 8
	}

	object LoadingSpinnerStyle {

		val loadingSpinner by cssClass()
		val smallSpinner by cssClass()

		init {
			addStyleToHead(
				"""
$loadingSpinner {
  display: inline-block;
  position: relative;
  --radius: 40px;
  --duration: 1.8s;
  --size: 8px;
  width: calc(var(--radius) * 2);
  height: calc(var(--radius) * 2);
}

$smallSpinner {
	--radius: 0.5em;
	--size: 3px;
}

$loadingSpinner div {
  animation: spin var(--duration) cubic-bezier(0.5, 0, 0.5, 1) infinite;
  position: absolute;
  --p: 0;
  --spread: 160deg;
  opacity: calc(var(--p) * 0.8);
  
  width: var(--size);
  height: var(--size);
  border-radius: 50%;
  --inner-radius: calc(var(--radius) - var(--size) * 0.5);
  
  background: ${CssProps.loadingSpinnerColor.v};
  animation-delay: calc(var(--duration) * var(--p) * -0.2);
  
}

@keyframes spin {
  0% {
    transform: ${ballTransform("0deg")}
  }
  100% {
  	transform: ${ballTransform("360deg")}
  }
}

			"""
			)
		}
	}
}

private fun ballTransform(rotationOffset: String): String = css("""translate(calc(var(--radius) - var(--size) * 0.5), calc(var(--radius) - var(--size) * 0.5)) rotate(calc(var(--p) * var(--spread) + $rotationOffset)) translateX(calc(var(--inner-radius)));""")

inline fun Context.largeSpinner(init: ComponentInit<LoadingSpinner> = {}): LoadingSpinner {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return LoadingSpinner(this).apply(init)
}

inline fun Context.smallSpinner(init: ComponentInit<LoadingSpinner> = {}): LoadingSpinner {
	contract { callsInPlace(init, InvocationKind.EXACTLY_ONCE) }
	return LoadingSpinner(this).apply {
		ballCount = 6
		addClass(LoadingSpinner.LoadingSpinnerStyle.smallSpinner)
		init()
	}
}
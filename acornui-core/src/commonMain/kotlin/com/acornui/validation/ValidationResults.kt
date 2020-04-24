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

package com.acornui.validation

import com.acornui.component.InputComponent
import com.acornui.component.style.StyleTag
import com.acornui.component.text.TextStyleTags

/**
 * The results of a validation operation.
 */
data class ValidationResults<out T>(

		/**
		 * The immutable form data.
		 */
		val data: T,

		/**
		 * A list of validation infos.
		 */
		val results: List<ValidationInfo>
) {

	/**
	 * True if the validation completed successfully and no results were errors.
	 */
	val success = results.success
}

/**
 * Info for a validated part.
 */
data class ValidationInfo(

		/**
		 * A display message.
		 */
		val message: String,

		/**
		 * The severity. If this is [ValidationLevel.ERROR] then [ValidationResults.success] will return false.
		 */
		val level: ValidationLevel,

		/**
		 * The name of the validated component.
		 */
		val name: String,

		/**
		 * The component responsible for this validation.
		 */
		val component: InputComponent<*>
)

typealias Validator<T> = suspend (T) -> ValidationResults<T>

/**
 * True if this validation info list contains no errors.
 */
val List<ValidationInfo>.success: Boolean
	get() = none { it.level == ValidationLevel.ERROR }

enum class ValidationLevel(val styleTag: StyleTag) {

	ERROR(TextStyleTags.error),
	WARNING(TextStyleTags.warning),
	INFO(TextStyleTags.info),
	SUCCESS(TextStyleTags.regular)
}

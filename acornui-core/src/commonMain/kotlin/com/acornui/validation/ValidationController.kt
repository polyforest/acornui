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

import com.acornui.async.cancellingJobProp
import com.acornui.component.style.StyleTag
import com.acornui.component.text.TextStyleTags
import com.acornui.di.Context
import com.acornui.di.ContextImpl
import com.acornui.observe.DataBindingRo
import com.acornui.observe.dataBinding
import com.acornui.recycle.Clearable
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

class ValidationController(owner: Context) : ContextImpl(owner), Clearable {

	private val _data = dataBinding<ValidationResults<*>?>(null)

	/**
	 * The validation results from [validate].
	 */
	val data: DataBindingRo<ValidationResults<*>?> = _data.asRo()

	private val _isBusy = dataBinding<Boolean>(false)
	val isBusy: DataBindingRo<Boolean> = _isBusy.asRo()

	private var validationJob by cancellingJobProp<Deferred<ValidationResults<*>>>()

	/**
	 * Executes the validator, then sets the validation results on [data] and returns the validation results.
	 *
	 * Calling this method consecutively before the previous validation finished will cancel the previous validation,
	 * returning results marked incomplete for the previous call.
	 *
	 * The current validation results will be set to null immediately, before validation begins.
	 *
	 * @see data
	 */
	suspend fun <T> validate(validator: suspend () -> ValidationResults<T>): ValidationResults<T> {
		_data.value = null
		val validationJob = async {
			validator()
		}
		this.validationJob = validationJob
		_isBusy.value = true
		validationJob.join()
		return if (validationJob.isCancelled) ValidationResults(null, isCancelled = true, results = emptyList())
		else {
			@Suppress("EXPERIMENTAL_API_USAGE")
			val results = validationJob.getCompleted()
			_data.value = results
			_isBusy.value = false
			results
		}
	}

	/**
	 * Clears the current data and cancels the pending validation job if there is one.
	 */
	override fun clear() {
		validationJob = null
		_data.value = null
	}

	override fun dispose() {
		clear()
		super.dispose()
	}

	companion object : Context.Key<ValidationController>
}

val Context.validationController: ValidationController
	get() = inject(ValidationController)

/**
 * The results of a validation operation.
 */
data class ValidationResults<T>(

		/**
		 * The original data being validated.
		 * This data should not be mutable.
		 */
		val data: T?,

		/**
		 * True if the validation was cancelled.
		 * If the validation was cancelled, this will be true and [success] will return false.
		 */
		val isCancelled: Boolean,

		/**
		 * A list of validation infos.
		 */
		val results: List<ValidationInfo>
) {

	/**
	 * True if the validation completed successfully and no results were errors.
	 */
	val success = !isCancelled && results.none { it.level == ValidationLevel.ERROR }
}

/**
 * Info for a validated part.
 */
data class ValidationInfo(

		/**
		 * A display message.
		 */
		val message: String = "",

		/**
		 * The severity. If this is [ValidationLevel.ERROR] then [ValidationResults.success] will return false.
		 */
		val level: ValidationLevel = ValidationLevel.ERROR,

		/**
		 * The id of the component responsible for providing the data part.
		 */
		val componentId: String? = null
)

enum class ValidationLevel(val styleTag: StyleTag) {

	ERROR(TextStyleTags.error),
	WARNING(TextStyleTags.warning),
	INFO(TextStyleTags.info),
	SUCCESS(TextStyleTags.regular)
}


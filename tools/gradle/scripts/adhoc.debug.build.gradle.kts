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

import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

typealias ProjectConsumer<T> = (Project) -> T
val logFindings by extra { it: Project ->
	val isCompositeRoot = it.gradle.includedBuilds.isNotEmpty() && it.gradle.parent == null
	val isCompositeNonRoot = it.gradle.includedBuilds.isEmpty() && it.gradle.parent != null
	val isCompositeIncludedRoot = isCompositeNonRoot && it.parent == null
	logger.quiet("------------------------------------------------------")
	logger.quiet("CURRENT PROJECT: ${it.name}")
	logger.quiet("CURRENT ROOT: ${it.rootProject.name}")
	logger.quiet("IS COMPOSITE_ROOT?: $isCompositeRoot")
	logger.quiet("IS COMPOSITE_NON_ROOT?: $isCompositeNonRoot")
	logger.quiet("IS COMPOSITE_INCLUDED_ROOT?: $isCompositeIncludedRoot")
	logger.quiet("INCLUDED BUILDS: ${it.gradle.includedBuilds.map { it.name }}")
	logger.quiet("GRADLE PARENT: ${it.gradle.parent}")
	logger.quiet("PROJECT PARENT: ${it.parent?.name}")
	logger.quiet("------------------------------------------------------")
}

val printTaskTypes by extra { it: Project ->
	logger.quiet(it.tasks.map { it.name to it::class.jvmName }
			.joinToString("\n") { "${it.first.padStart(30)}:  ${it.second}" })
}

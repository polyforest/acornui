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

package com.acornui.time

/**
 * Returns the number of milliseconds elapsed since 1 January 1970 00:00:00 UTC.
 */
expect fun nowMs(): Long

/**
 * Returns the number of seconds elapsed since 1 January 1970 00:00:00 UTC.
 */
fun nowS(): Double = (nowMs().toDouble() / 1000.0)

/**
 * Returns the current value of the running high-resolution time source, in nanoseconds from the time the
 * application started.
 *
 * The resolution varies per backend.
 * On the JS backend, this is accurate to the nearest 1 milliseconds. see `performance.now`
 * For the JVM side, see `System.nanoTime`
 *
 * See [https://developer.mozilla.org/en-US/docs/Web/API/Performance/now]
 */
expect fun nanoElapsed(): Long

/**
 * [nanoElapsed] divided by `1,000,000`
 * @see nanoElapsed
 */
fun msElapsed(): Long = nanoElapsed() / 1_000_000L
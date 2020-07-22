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

import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

/**
 * Returns the number of seconds elapsed since 1 January 1970 00:00:00 UTC.
 */
fun nowS(): Double = (nowMs().toDouble() / 1000.0)

/**
 * Returns the number of milliseconds elapsed since 1 January 1970 00:00:00 UTC.
 */
fun nowMs(): Long {
    return (js("Date.now()") as Double).toLong()
}

fun markNow() = TimeSource.Monotonic.markNow()

/**
 * Convert this duration to its millisecond value.
 * Positive durations are coerced at least `1`.
 */
fun Duration.toDelayMillis(): Long =
    if (this > Duration.ZERO) toLongMilliseconds().coerceAtLeast(1) else 0

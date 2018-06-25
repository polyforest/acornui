/*
 * Copyright 2018 Nicholas Bilyk
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

package com.acornui.particle

import com.acornui.core.graphics.BlendMode
import com.acornui.core.time.time
import com.acornui.math.Easing
import com.acornui.serialization.JsonSerializer
import com.acornui.test.MockTimeProvider
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class ParticleEffectSerializerTest {


	@Before
	fun setUp() {
		time = MockTimeProvider()
	}

	@Test
	fun serializeToFro() {
		val effect = ParticleEffect(emitters = listOf(
				ParticleEmitter(
						name = "emitter1",
						enabled = true,
						loops = true,
						duration = EmitterDuration(
								duration = FloatRange(3f, 5f),
								delayBefore = FloatRange(1f),
								delayAfter = FloatRange(2.5f)
						),
						count = 100,
						emissionRate = FloatTimeline(0, "emissionRate", false, FloatRange(3f, 10f, Easing.circle), FloatRange(20f, 30f, Easing.linear), listOf(
								TimelineValue(0.1f, 0.2f),
								TimelineValue(0.3f, 0.4f)
						), true),
						particleLifeExpectancy = FloatTimeline(0, "particleLifeExpectancy", true, FloatRange(5f, 20f, Easing.elastic), FloatRange(20f, 30f, Easing.exp5), listOf(
								TimelineValue(0.7f, 0.2f),
								TimelineValue(0.8f, 0.5f)
						), true),
						spawnLocation = PointSpawn(FloatRange(3f, 4f), FloatRange(5f, 7f), FloatRange(2f, 6f)),
						blendMode = BlendMode.NORMAL,
						premultipliedAlpha = false,
						imageEntries = listOf(
								ParticleImageEntry(0.1f, "particle.png")
						),
						orientToForwardDirection = false,
						propertyTimelines = listOf(
								FloatTimeline(0, "x", false, FloatRange(15f, 25f, Easing.linear), FloatRange(20f, 50f, Easing.exp5), listOf(
										TimelineValue(0.7f, 0.2f),
										TimelineValue(0.8f, 0.5f),
										TimelineValue(0.9f, 0.5f)
								), true),
								FloatTimeline(0, "y", true, FloatRange(5f, 20f, Easing.elastic), FloatRange(20f, 30f, Easing.exp5), listOf(), true)
						)

				)
		))

		val json = JsonSerializer.write(effect, ParticleEffectSerializer)
		val effect2 = JsonSerializer.read(json, ParticleEffectSerializer)

//		assertEquals(effect, effect2)
	}
}
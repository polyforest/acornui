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

package com.acornui.particle

import com.acornui.asset.loadText
import kotlinx.serialization.json.Json
import kotlin.test.Test

class ParticleEffectTest {

	//language=JSON
	private val effectJson = """
		{
			"version": "0.2.0",
			"emitters": [
				{
					"name": "leaves",
					"enabled": true,
					"loops": false,
					"duration": {
						"duration": {
							"min": 5,
							"max": 5,
							"easing": "linear"
						},
						"delayBefore": {
							"min": 0,
							"max": 0,
							"easing": "linear"
						},
						"delayAfter": {
							"min": 0,
							"max": 0,
							"easing": "linear"
						}
					},
					"count": 200,
					"emissionRate": {
						"low": {
							"min": 0,
							"max": 0,
							"easing": "linear"
						},
						"high": {
							"min": 150,
							"max": 150,
							"easing": "linear"
						},
						"property": "emissionRate",
						"relative": false,
						"timeline": [ 0, 1, 0.22279924154281616, 1, 0.41279375553131104, 0, 1, 0 ]
					},
					"particleLifeExpectancy": {
						"low": {
							"min": 12,
							"max": 12,
							"easing": "linear"
						},
						"high": {
							"min": 0.7999999523162842,
							"max": 0.7999999523162842,
							"easing": "linear"
						},
						"property": "particleLifeExpectancy",
						"relative": false,
						"timeline": [ ]
					},
					"blendMode": "normal",
					"premultipliedAlpha": false,
					"imageEntries": [
						{
							"path": "diamond",
							"time": 0
						}
					],
					"orientToForwardDirection": true,
					"propertyTimelines": [
						{
							"low": {
								"min": -3,
								"max": -3,
								"easing": "linear"
							},
							"high": {
								"min": -1,
								"max": 1,
								"easing": "linear"
							},
							"property": "velocityX",
							"relative": true,
							"timeline": [ 0, 0, 0.2118644118309021, 1, 0.48797157406806946, 0.03932584449648857, 0.6834335923194885, 1 ],
							"useEmitterDuration": true
						},
						{
							"low": {
								"min": 0,
								"max": 0,
								"easing": "linear"
							},
							"high": {
								"min": 1,
								"max": 1,
								"easing": "linear"
							},
							"property": "colorA",
							"relative": false,
							"timeline": [ 0, 0, 0.17129509150981903, 1, 0.8015885949134827, 1, 1, 0 ]
						},
						{
							"low": {
								"min": 0.699999988079071,
								"max": 0.949999988079071,
								"easing": "linear"
							},
							"high": {
								"min": 1,
								"max": 1,
								"easing": "linear"
							},
							"property": "colorR",
							"relative": false,
							"timeline": [ ]
						},
						{
							"low": {
								"min": 0.10000000149011612,
								"max": 0.800000011920929,
								"easing": "linear"
							},
							"high": {
								"min": 1,
								"max": 1,
								"easing": "linear"
							},
							"property": "colorG",
							"relative": false,
							"timeline": [ ]
						},
						{
							"low": {
								"min": 0,
								"max": 0,
								"easing": "linear"
							},
							"high": {
								"min": 1,
								"max": 1,
								"easing": "linear"
							},
							"property": "colorB",
							"relative": false,
							"timeline": [ 0, 0.15980058908462524 ]
						},
						{
							"low": {
								"min": -0.20000000298023224,
								"max": 3,
								"easing": "linear"
							},
							"high": {
								"min": -0.09999999403953552,
								"max": 0.09999999403953552,
								"easing": "linear"
							},
							"property": "velocityY",
							"relative": true,
							"timeline": [ 0, 0, 0.2902587056159973, 0.10112392902374268, 0.45174065232276917, 0.8539327383041382, 0.7910571694374084, 0.6910112500190735, 1, 0 ]
						},
						{
							"low": {
								"min": 3.1415927410125732,
								"max": 3.1415927410125732,
								"easing": "linear"
							},
							"high": {
								"min": 0,
								"max": 0,
								"easing": "linear"
							},
							"property": "forwardDirectionZ",
							"relative": false,
							"timeline": [ ]
						},
						{
							"low": {
								"min": -0.009999999776482582,
								"max": -0.009999999776482582,
								"easing": "linear"
							},
							"high": {
								"min": -0.029999999329447746,
								"max": -0.03999999910593033,
								"easing": "linear"
							},
							"property": "forwardDirectionVelocityZ",
							"relative": false,
							"timeline": [ 0, 1, 0.16265718638896942, 1, 0.3266812562942505, 0 ]
						},
						{
							"low": {
								"min": 0,
								"max": 3,
								"easing": "linear"
							},
							"high": {
								"min": 2,
								"max": 3.5,
								"easing": "pow3In"
							},
							"property": "forwardVelocity",
							"relative": true,
							"timeline": [ 0, 1, 0.35819873213768005, 0.915730357170105, 1, 0.10674172639846802 ]
						},
						{
							"low": {
								"min": 0,
								"max": 0,
								"easing": "linear"
							},
							"high": {
								"min": 0,
								"max": 0,
								"easing": "linear"
							},
							"property": "y",
							"relative": false,
							"timeline": [ ]
						},
						{
							"low": {
								"min": 0.20000000298023224,
								"max": 0.5,
								"easing": "linear"
							},
							"high": {
								"min": -0.4000000059604645,
								"max": 0.4000000059604645,
								"easing": "linear"
							},
							"property": "velocityY",
							"relative": true,
							"timeline": [ 0.24805033206939697, 0.07865168899297714, 0.4369628429412842, 0.8707864880561829, 0.6538015604019165, 0.9213483333587646, 0.7227956652641296, 0 ],
							"useEmitterDuration": true
						},
						{
							"low": {
								"min": 0,
								"max": 500,
								"easing": "linear"
							},
							"high": {
								"min": 0,
								"max": 0,
								"easing": "linear"
							},
							"property": "z",
							"relative": false,
							"timeline": [ ]
						},
						{
							"low": {
								"min": 0,
								"max": 0,
								"easing": "linear"
							},
							"high": {
								"min": -2,
								"max": 2,
								"easing": "linear"
							},
							"property": "velocityZ",
							"relative": false,
							"timeline": [ 0, 0, 0.2984808385372162, 0.7528089880943298, 0.5712646245956421, 0.550561785697937, 0.8064912557601929, 0 ],
							"useEmitterDuration": true
						},
						{
							"low": {
								"min": -100,
								"max": 100,
								"easing": "linear"
							},
							"high": {
								"min": 0,
								"max": 0,
								"easing": "linear"
							},
							"property": "x",
							"relative": false,
							"timeline": [ ]
						}
					]
				}
			]
		}
	"""

	@Test
	fun deserialize() {
		val json = Json.nonstrict
		val pEffect = json.parse(ParticleEffect.serializer(), effectJson)
		println(pEffect)
	}
}
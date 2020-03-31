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

package com.acornui.graphic

import com.acornui.gl.core.*
import com.acornui.math.Matrix3
import com.acornui.math.Matrix4

class LightingShaderWithNormalMap(gl: CachedGl20, numPointLights: Int, numShadowPointLights: Int) : ShaderProgramBase(
		gl, vertexShaderSrc = """
$DEFAULT_SHADER_HEADER

struct PointLight {
	float radius;
	vec3 position;
	vec3 color;
};

attribute vec3 a_position;
attribute vec3 a_normal;
attribute vec3 a_tangent;
attribute vec3 a_bitangent;
attribute vec4 a_colorTint;
attribute vec2 a_texCoord0;

uniform mat4 u_projTrans;
uniform mat3 u_normalTrans;
uniform mat4 u_modelTrans;
uniform mat4 u_directionalLightMvp;
uniform vec3 u_directionalLightDir;
uniform PointLight u_pointLights[$numPointLights];

varying vec4 v_worldPosition;
varying vec4 v_colorTint;
varying vec2 v_texCoord;
varying vec3 v_tsDirectionalLightDir;
varying vec4 v_directionalShadowCoord;
varying vec3 v_pointLightDirectionsTs[$numPointLights];

mat3 transpose(in mat3 mat) {
    vec3 i0 = mat[0];
    vec3 i1 = mat[1];
    vec3 i2 = mat[2];

    return mat3(
        vec3(i0.x, i1.x, i2.x),
        vec3(i0.y, i1.y, i2.y),
        vec3(i0.z, i1.z, i2.z)
    );
}

void main() {
	v_colorTint = a_colorTint;
	v_texCoord = a_texCoord0;
	v_worldPosition = u_modelTrans * vec4(a_position, 1.0);

	vec3 t = normalize(u_normalTrans * vec3(1.0, 0.0, 0.0));
    vec3 b = normalize(u_normalTrans * vec3(0.0, 0.0, -1.0));
    vec3 n = normalize(u_normalTrans * vec3(0.0, -1.0, 0.0));

	//vec3 t = normalize(u_normalTrans * a_tangent);
    //vec3 b = normalize(u_normalTrans * a_bitangent);
    //vec3 n = normalize(u_normalTrans * a_normal);
    mat3 tbn = transpose(mat3(t, b, n));

	gl_Position =  u_projTrans * v_worldPosition;
	v_tsDirectionalLightDir = tbn * u_directionalLightDir;
	v_directionalShadowCoord = u_directionalLightMvp * v_worldPosition;

	PointLight pointLight;
	for (int i = 0; i < $numPointLights; i++) {
		pointLight = u_pointLights[i];
		if (pointLight.radius > 1.0) {
			v_pointLightDirectionsTs[i] = normalize(tbn * (pointLight.position - v_worldPosition.xyz));
		}
	}
}
""", fragmentShaderSrc = """
$DEFAULT_SHADER_HEADER

struct PointLight {
	float radius;
	vec3 position;
	vec3 color;
};

varying vec4 v_worldPosition;
varying vec4 v_colorTint;
varying vec2 v_texCoord;
varying vec3 v_tsDirectionalLightDir;
varying vec4 v_directionalShadowCoord;
varying vec3 v_pointLightDirectionsTs[$numPointLights];

uniform int u_shadowsEnabled;
uniform vec2 u_resolutionInv;
uniform vec4 u_ambient;
uniform vec4 u_directional;
uniform sampler2D u_texture;
uniform sampler2D u_textureNormal;
uniform sampler2D u_directionalShadowMap;

uniform bool u_useColorTrans;
uniform mat4 u_colorTrans;
uniform vec4 u_colorOffset;

// Point lights
uniform PointLight u_pointLights[$numPointLights];
${if (numShadowPointLights > 0) "uniform samplerCube u_pointLightShadowMaps[$numShadowPointLights];" else ""}

uniform vec2 poissonDisk[4];

$UNPACK_FLOAT

float getShadowDepth(const in vec2 coord) {
	vec4 c = texture2D(u_directionalShadowMap, coord);
	return unpackFloat(c);
}

vec3 getDirectionalColor(vec3 normal) {
	float cosTheta = clamp(dot(normal, -v_tsDirectionalLightDir), 0.05, 1.0);
	if (u_shadowsEnabled == 0 || u_directional.rgb == vec3(0.0)) return cosTheta * u_directional.rgb;
	float visibility = 0.0;
	float shadow = getShadowDepth(v_directionalShadowCoord.xy);
	float bias = 0.002;
	float testZ = v_directionalShadowCoord.z - bias;
	if (testZ >= unpackFloat(vec4(0.0, 0.0, 1.0, 1.0)))
    	return cosTheta * u_directional.rgb;

	if (shadow >= testZ) visibility += 0.2;
	for (int i = 0; i < 4; i++) {
		shadow = getShadowDepth((v_directionalShadowCoord.xy + poissonDisk[i] * u_resolutionInv));
		if (shadow >= testZ) visibility += 0.2;
	}

	return visibility * cosTheta * u_directional.rgb;
}

vec3 getPointColor(vec3 normal) {
	vec3 pointColor = vec3(0.0);
	PointLight pointLight;
	vec3 lightToPixel;
	vec3 lightToPixelN;
	float attenuation;
	float distance;
	float shadow;
	float testZ;

	float bias = -0.005;
	float maxD = unpackFloat(vec4(0.0, 0.0, 1.0, 1.0));

	for (int i = 0; i < $numPointLights; i++) {
		pointLight = u_pointLights[i];
		if (pointLight.radius > 1.0) {
			lightToPixel = v_worldPosition.xyz - pointLight.position;
			lightToPixelN = normalize(lightToPixel);
			distance = length(lightToPixel) / pointLight.radius;
			testZ = distance - bias;

			attenuation = 1.0 - clamp(distance, 0.0, 1.0);
			float cosTheta = clamp(dot(normal, normalize(v_pointLightDirectionsTs[i])), 0.0, 1.0);

			if (u_shadowsEnabled == 0 || i >= $numShadowPointLights) {
				pointColor += cosTheta * pointLight.color * attenuation * attenuation;
			} else {
				${if (numShadowPointLights > 0) """
				shadow = unpackFloat(textureCube(u_pointLightShadowMaps[i], lightToPixelN));

				if (testZ >= maxD || shadow >= testZ) {
					pointColor += cosTheta * pointLight.color * attenuation * attenuation;
				}
				""" else ""}
			}
		}
	}

	return pointColor;
}

void main() {
	vec3 normal = normalize(texture2D(u_textureNormal, v_texCoord).rgb * 2.0 - 1.0);
	vec3 directional = getDirectionalColor(normal);
	vec3 point = getPointColor(normal);

	vec4 diffuseColor = v_colorTint * texture2D(u_texture, v_texCoord);

	vec4 final = vec4(clamp(u_ambient.rgb + directional + point, 0.0, 1.3) * diffuseColor.rgb, diffuseColor.a);
	if (u_useColorTrans) {
		gl_FragColor = u_colorTrans * final + u_colorOffset;
	} else {
		gl_FragColor = final;
	}

	if (gl_FragColor.a < 0.01) discard;
}


""",
		vertexAttributes = mapOf(
VertexAttributeLocation.POSITION to CommonShaderAttributes.A_POSITION,
VertexAttributeLocation.NORMAL to CommonShaderAttributes.A_NORMAL,
VertexAttributeLocation.TANGENT to CommonShaderAttributes.A_TANGENT,
VertexAttributeLocation.BITANGENT to CommonShaderAttributes.A_BITANGENT,
VertexAttributeLocation.COLOR_TINT to CommonShaderAttributes.A_COLOR_TINT,
VertexAttributeLocation.TEXTURE_COORD to CommonShaderAttributes.A_TEXTURE_COORD + "0"
)) {

	override fun initUniforms(uniforms: Uniforms) {
		// Poisson disk
		uniforms.put("poissonDisk[0]", -0.94201624f, -0.39906216f)
		uniforms.put("poissonDisk[1]", 0.94558609f, -0.76890725f)
		uniforms.put("poissonDisk[2]", -0.09418410f, -0.92938870f)
		uniforms.put("poissonDisk[3]", 0.34495938f, 0.29387760f)
		uniforms.put("u_projTrans", Matrix4.IDENTITY)
		uniforms.put("u_directionalLightMvp", Matrix4.IDENTITY)
		uniforms.put("u_modelTrans", Matrix4.IDENTITY)
		uniforms.put("u_normalTrans", Matrix3.IDENTITY)
	}
}

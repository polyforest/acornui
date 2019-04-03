/*
 * Copyright 2019 PolyForest
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

val GRADLE_VERSION: String by extra
val PRODUCT_VERSION: String by extra
val PRODUCT_GROUP: String by extra

version = PRODUCT_VERSION
group = PRODUCT_GROUP

tasks.withType<Wrapper> {
    gradleVersion = GRADLE_VERSION
    distributionType = Wrapper.DistributionType.ALL
}

afterEvaluate {
    val clean = tasks.withType(Delete::class).tryNamed(BasePlugin.CLEAN_TASK_NAME) ?: tasks.register<Delete>(BasePlugin.CLEAN_TASK_NAME)
    clean {
        group = "build"
        description = """
            Deletes:
            ${delete.joinToString("\n") {
            if (it is File)
                it.relativeToOrSelf(projectDir).path
            else
                it.toString()
        }}
        
        (all files relative to project directory unless absolute)
        """.trimIndent()

        delete(file("out/"))
    }
}

fun <T : Task> TaskCollection<T>.tryNamed(name: String): TaskProvider<T>? {
    return try {
        named(name)
    } catch (e: UnknownTaskException) {
        null
    }
}

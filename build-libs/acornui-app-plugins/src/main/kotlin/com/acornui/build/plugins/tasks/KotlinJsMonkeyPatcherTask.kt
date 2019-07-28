@file:Suppress("UnstableApiUsage")

package com.acornui.build.plugins.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction

open class KotlinJsMonkeyPatcherTask @javax.inject.Inject constructor(objects: ObjectFactory) : DefaultTask() {

    @InputDirectory
    val sourceDir = objects.directoryProperty()

    private val alwaysTrue = "function alwaysTrue() { return true; }"

    @TaskAction
    fun execute() {
        sourceDir.asFile.get().listFiles()?.forEach { srcFile ->
            if (srcFile.extension.equals("js", ignoreCase = true)) {
                val src = srcFile.readText()
                if (src.endsWith(alwaysTrue)) {
                    logger.warn("Already patched ${srcFile.path}")
                    return@forEach
                }
                logger.info("Patching file ${srcFile.path}")
                srcFile.writeText(optimizeProductionCode(src))
            }
        }
    }

    /**
     * Makes it all go weeeeee!
     */
    private fun optimizeProductionCode(src: String): String {
        var result = src
        result = simplifyArrayListGet(result)
        result = simplifyArrayListSet(result)
        result = stripCce(result)
        result = stripRangeCheck(result)
        if (result != src)
            result += alwaysTrue
        return result
    }

    /**
     * Strips type checking that only results in a class cast exception.
     */
    private fun stripCce(src: String): String {
        return Regex("""Kotlin\.is(Type|Array|Char|CharSequence|Number)(\((.*?) \? tmp\$(?:_\d+)? : (Kotlin\.)?throw(\w*?)\(\))""").replace(
            src,
            "alwaysTrue\$2"
        )
    }

    private fun stripRangeCheck(src: String): String {
        return src.replace("this.rangeCheck_2lys7f${'$'}_0(index)", "index")
    }

    private fun simplifyArrayListGet(src: String): String {
        return Regex("""ArrayList\.prototype\.get_za3lpa\$[\s]*=[\s]*function[\s]*\(index\)[\s]*\{([^}]+)};""")
            .replace(src) {
                """ArrayList.prototype.get_za3lpa$ = function(index) { return this.array_hd7ov6${'$'}_0[index] };"""
            }
    }

    private fun simplifyArrayListSet(src: String): String {
        return Regex("""ArrayList\.prototype\.set_wxm5ur\$[\s]*=[\s]*function[\s]*\(index, element\)[\s]*\{([^}]+)};""")
            .replace(src) {
                """
						ArrayList.prototype.set_wxm5ur${'$'} = function (index, element) {
			  				var previous = this.array_hd7ov6${'$'}_0[index];
			  				this.array_hd7ov6${'$'}_0[index] = element;
			  				return previous;
						};
					""".trimIndent()
            }
    }
}
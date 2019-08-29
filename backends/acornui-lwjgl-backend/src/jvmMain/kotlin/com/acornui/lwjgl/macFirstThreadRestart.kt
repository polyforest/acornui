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

package com.acornui.lwjgl

import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.management.ManagementFactory
import java.util.*

/**
 * http://www.java-gaming.org/topics/starting-jvm-on-mac-with-xstartonfirstthread-programmatically/37697/view.html
 * On a mac, the jvm process must be started on the first Thread.
 */
fun macFirstThreadRestart(): Boolean {

    val osName = System.getProperty("os.name")

    // if not a mac return false
    if (!osName.startsWith("Mac") && !osName.startsWith("Darwin")) {
        return false
    }

    // get current jvm process pid
    val pid = ManagementFactory.getRuntimeMXBean().name.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
    // get environment variable on whether XstartOnFirstThread is enabled
    val env = System.getenv("JAVA_STARTED_ON_FIRST_THREAD_$pid")

    // if environment variable is "1" then XstartOnFirstThread is enabled
    if (env != null && env == "1") {
        return false
    }

    // restart jvm with -XstartOnFirstThread
    val separator = System.getProperty("file.separator")
    val classpath = System.getProperty("java.class.path")
    val mainClass = System.getenv("JAVA_MAIN_CLASS_$pid")
    val jvmPath = System.getProperty("java.home") + separator + "bin" + separator + "java"

    val inputArguments = ManagementFactory.getRuntimeMXBean().inputArguments

    val jvmArgs = ArrayList<String>()

    jvmArgs.add(jvmPath)
    jvmArgs.add("-XstartOnFirstThread")
    jvmArgs.addAll(inputArguments)
    jvmArgs.add("-cp")
    jvmArgs.add(classpath)
    jvmArgs.add(mainClass)

    // if you don't need console output, just enable these two lines
    // and delete bits after it. This JVM will then terminate.
    //ProcessBuilder processBuilder = new ProcessBuilder(jvmArgs);
    //processBuilder.start();

    try {
        val processBuilder = ProcessBuilder(jvmArgs)
        processBuilder.redirectErrorStream(true)
        val process = processBuilder.start()

        val `is` = process.inputStream
        val isr = InputStreamReader(`is`)
        val br = BufferedReader(isr)

        br.lines().forEach { println(it) }

        process.waitFor()
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return true
}
package com.acornui

import com.acornui.logging.Log

var uncaughtExceptionHandler: (error: Throwable) -> Unit = { Log.error(it) }
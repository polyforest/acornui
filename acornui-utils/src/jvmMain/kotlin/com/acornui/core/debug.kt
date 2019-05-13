package com.acornui.core

/**
 * A flag for enabling various debugging features like debug logging.
 */
actual val debug: Boolean by lazy { System.getProperty("debug")?.toLowerCase() == "true" }
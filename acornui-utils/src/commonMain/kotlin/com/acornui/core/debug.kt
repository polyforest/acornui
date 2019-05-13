package com.acornui.core

/**
 * A flag for enabling various debugging features like debug logging.
 * This will be true if:
 * On the JS backend debug=true exists as a querystring parameter.
 * On the JVM backend -Ddebug=true exists as a vm parameter.
 */
expect val debug: Boolean
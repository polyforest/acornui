package com.acornui.component

import com.acornui.signal.Cancel
import com.acornui.signal.Signal

interface Closeable {
	val closing: Signal<(Closeable, Cancel) -> Unit>
	val closed: Signal<(Closeable) -> Unit>
}
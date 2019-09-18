package com.acornui.headless

import com.acornui.io.file.Directory
import com.acornui.io.file.FileEntry
import com.acornui.io.file.Files

object MockFiles : Files {

	override fun getFile(path: String): FileEntry? = null

	override fun getDir(path: String): Directory? = null
}
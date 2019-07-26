package com.acornui.core.io.file

import com.acornui.io.file.ManifestUtil
import org.junit.Ignore
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FilesImplTest {

	@Ignore("KT-24463")
	@Test
	fun getDir() {
		val manifest = ManifestUtil.createManifest(File("src/jvmTest/resources"))
		val files = FilesImpl(manifest)
		assertNotNull(files.getDir("testAssets"))
		assertNotNull(files.getDir("testAssets/testA"))
		assertNotNull(files.getDir("testAssets/testA/testB"))
		assertNotNull(files.getDir("testAssets/testA/testB/"))
		assertNotNull(files.getFile("testAssets/testA/testB/a_3.txt"))
		assertNotNull(files.getFile("testAssets/testA/testB/b_3.txt"))
		assertEquals(2, files.getDir("testAssets/testA/testB")?.totalFiles)
		assertEquals(2, files.getDir("testAssets/testA/testB")?.files?.size)

	}
}
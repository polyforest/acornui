package com.acornui.build.plugins.util

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import kotlin.experimental.and

fun getFileChecksum(digest: MessageDigest, file: File): String {
    //Get file input stream for reading the file content
    val fis = FileInputStream(file)

    //Create byte array to read data in chunks
    val byteArray = ByteArray(1024)
    var bytesCount: Int

    //Read file data and update in message digest
    while (true) {
        bytesCount = fis.read(byteArray)
        if (bytesCount == -1) break
        digest.update(byteArray, 0, bytesCount)
    }

    //close the stream; We don't need it now.
    fis.close()

    //Get the hash's bytes
    val bytes = digest.digest()

    //This bytes[] has bytes in decimal format;
    //Convert it to hexadecimal format
    val sb = StringBuilder()
    for (i in 0..bytes.lastIndex) {
        sb.append(((bytes[i] and 0xff.toByte()) + 0x100.toByte()).toString(16).substring(1))
    }
    //return complete hash
    return sb.toString()
}
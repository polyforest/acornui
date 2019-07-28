package com.acornui.build.plugins.utils

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.SftpATTRS
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import java.io.File
import com.jcraft.jsch.Logger as JschLogger

private lateinit var logger: Logger

/**
 * Creates an SFTP connection to the Acorn UI Artifact server to publish artifacts.
 */
fun jschBandbox(logger: Logger, inner: (channel: ChannelSftp) -> Unit) {
	com.acornui.build.plugins.utils.logger = logger
	JSch.setLogger(GradleJschLoggingAdapter(com.acornui.build.plugins.utils.logger))
	val jsch = JSch()
	val ftpUsername: String = System.getenv("BANDBOX_FTP_USERNAME")
	val ftpPassword: String = System.getenv("BANDBOX_FTP_PASSWORD")

	val session = jsch.getSession(ftpUsername, "bandbox.dreamhost.com")
	session.setConfig("StrictHostKeyChecking", "no")
	session.setPassword(ftpPassword)
	session.connect()
	val channel = session.openChannel("sftp") as ChannelSftp
	channel.connect()
	logger.lifecycle("SFTP connected")
	try {
		inner(channel)
	} finally {
		channel.disconnect()
		session.disconnect()
		logger.lifecycle("SFTP disconnected")
	}
}

fun ChannelSftp.deleteRecursively(remoteDir: String) {
	if (isDir(remoteDir)) {
		ls(remoteDir).forEach { entry ->
			entry as ChannelSftp.LsEntry
			if (!(entry.filename == "." || entry.filename == "..")) {
				val file = "$remoteDir/${entry.filename}"
				if (entry.attrs.isDir) {
					deleteRecursively(file)
				} else {
					logger.lifecycle("rm $file")
					rm(file)
				}
			}
		}
		logger.lifecycle("rmdir $remoteDir")
		rmdir(remoteDir)
	}
}

/**
 * Creates a temporary directory, then invokes [inner].
 * If [inner] is successful: Delete [remoteDir], then rename the temporary directory to replace the old remote.
 * If [inner] is unsuccessful: Delete the temporary directory.
 */
fun ChannelSftp.useTmpDirThenSwap(remoteDir: String, inner: (dir: String) -> Unit) {
	val remoteDirTmp = "${remoteDir}_tmp${System.currentTimeMillis()}"
	mkdir(remoteDirTmp)
	try {
		inner(remoteDirTmp)
		deleteRecursively(remoteDir)
		logger.lifecycle("rename $remoteDirTmp $remoteDir")
		rename(remoteDirTmp, remoteDir)
	} catch (e: Throwable) {
		deleteRecursively(remoteDirTmp)
	}
}

/**
 * Creates the remote directories for the [destination].
 * @param destination A '/' separated path.
 */
fun ChannelSftp.mkdirs(destination: String) {
	var path = ""
	destination.split("/").forEach {
		path += it
		if (!exists(path)) {
			logger.lifecycle("mkdir $path")
			mkdir(path)
		}
		path += "/"
	}
}

/**
 * Uploads the complete directory to the remote destination.
 * This will overwrite any existing files.
 * The files will be uploaded as temporary files first, then when all files are done uploading, they will then
 * replace the existing files at the target path. This is to reduce downtime of artifacts.
 *
 * @param file A directory to upload.
 * @param destination A '/' separated path.
 *
 * @see deleteRecursively
 */
fun ChannelSftp.uploadDir(file: File, destination: String) {
	val replacements = mutableListOf<Pair<String, String>>()
	try {
		uploadDirTempFiles(file, destination, replacements)
		for ((childDestinationTmp, childDestination) in replacements) {
			if (exists(childDestination)) {
				logger.lifecycle("rm  $childDestination")
				rm(childDestination)
			}
			logger.lifecycle("rename  $childDestinationTmp $childDestination")
			rename(childDestinationTmp, childDestination)
		}
	} catch (e: Throwable) {
		// There was an error with the upload. Delete all temporary files.
		for ((childDestinationTmp, childDestination) in replacements) {
			if (exists(childDestinationTmp)) {
				logger.lifecycle("rm  $childDestinationTmp")
				rm(childDestinationTmp)
			}
		}
		throw e
	}

}

/**
 * Uploads the directory as a set of temporary files.
 */
private fun ChannelSftp.uploadDirTempFiles(file: File, destination: String, replacements: MutableList<Pair<String, String>>) {
	if (!file.exists()) return
	if (!file.isDirectory) throw IllegalArgumentException("${file.path} is not a directory.")
	val children = file.listFiles()!!
	mkdirs(destination)
	children.forEach {
		val childDestination = "$destination/${it.name}"
		if (it.isDirectory) {
			uploadDir(it, childDestination)
		} else {
			mkdirs(destination)
			val childDestinationTmp = "${childDestination}_tmp${System.currentTimeMillis()}"
			replacements.add(childDestinationTmp to childDestination)
			logger.lifecycle("put  ${it.path}, $childDestinationTmp")
			put(it.path, childDestinationTmp)
		}
	}
}

fun ChannelSftp.exists(path: String): Boolean {
	return statOrNull(path) != null
}

fun ChannelSftp.isDir(path: String): Boolean {
	return statOrNull(path)?.isDir == true
}

fun ChannelSftp.statOrNull(path: String): SftpATTRS? {
	return try {
		stat(path)
	} catch (e: Throwable) {
		null
	}
}

private class GradleJschLoggingAdapter(private val logger: Logger) : JschLogger {

	override fun isEnabled(level: Int): Boolean = true

	override fun log(level: Int, message: String?) {
		val logLevel: LogLevel = when (level) {
			JschLogger.DEBUG -> LogLevel.DEBUG
			JschLogger.INFO -> LogLevel.INFO
			JschLogger.WARN -> LogLevel.WARN
			JschLogger.ERROR -> LogLevel.ERROR
			JschLogger.FATAL -> LogLevel.ERROR
			else -> LogLevel.QUIET
		}
		logger.log(logLevel, message)
	}
}
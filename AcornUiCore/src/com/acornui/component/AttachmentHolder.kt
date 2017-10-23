package com.acornui.component

/**
 * An AttachmentHolder can contain a map of arbitrary objects. This is used to attach reusable behaviors to components
 * such as drag and drop, toss scrolling, user interaction.
 */
interface AttachmentHolder {

	fun <T : Any> getAttachment(key: Any): T?

	fun setAttachment(key: Any, value: Any)

	/**
	 * Removes an attachment added via [setAttachment]
	 */
	fun <T : Any> removeAttachment(key: Any): T?
}

fun <T : Any> AttachmentHolder.createOrReuseAttachment(key: Any, factory: () -> T): T {
	val existing = getAttachment<T>(key)
	if (existing != null) {
		return existing
	} else {
		val newAttachment = factory()
		setAttachment(key, newAttachment)
		return newAttachment
	}
}
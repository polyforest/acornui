package org.intellij.lang.annotations

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.TYPE)
annotation class Language(
    val value: String,
    val prefix: String = "",
    val suffix: String = ""
)
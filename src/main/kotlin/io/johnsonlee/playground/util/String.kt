package io.johnsonlee.playground.util

import java.io.File

fun String.capitalized(): String {
    return this[0].uppercase() + this.substring(1)
}

@JvmSynthetic
internal fun String.toCanonicalFile() = File(this).canonicalFile

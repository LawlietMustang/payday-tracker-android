package com.paydaytracker.app

/** Describes whether the latest known data has a successfully stored copy. */
internal fun backupDisplayStatus(current: String, automatic: String, status: String,
    enabled: Boolean, manual: String): String = when {
    current.isNotEmpty() && current == manual -> "saved"
    current.isNotEmpty() && current == automatic && status in setOf("saved", "off") -> "saved"
    status == "error" -> "error"
    enabled && status == "saving" -> "saving"
    enabled -> "pending"
    manual.isNotEmpty() || automatic.isNotEmpty() -> "outdated"
    else -> "off"
}

package com.example.mymedicinenotebook2

import android.content.Context
import java.security.MessageDigest

object QrFingerprintStore {

    private const val PREFS_NAME =
        "com.example.mymedicinenotebook2.QR_FINGERPRINTS"

    private const val KEY_FINGERPRINTS =
        "fingerprints"

    fun contains(
        context: Context,
        qrData: String
    ): Boolean {

        val fingerprints =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
                .getStringSet(
                    KEY_FINGERPRINTS,
                    emptySet()
                )
                ?: emptySet()

        return fingerprints.contains(
            fingerprint(qrData)
        )
    }

    fun rememberAll(
        context: Context,
        qrDataList: List<String>
    ) {

        if (qrDataList.isEmpty()) {
            return
        }

        val preferences =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val fingerprints =
            preferences
                .getStringSet(
                    KEY_FINGERPRINTS,
                    emptySet()
                )
                ?.toMutableSet()
                ?: mutableSetOf()

        qrDataList.forEach { qrData ->
            fingerprints.add(
                fingerprint(qrData)
            )
        }

        preferences
            .edit()
            .putStringSet(
                KEY_FINGERPRINTS,
                fingerprints
            )
            .commit()
    }
    fun clearAll(
        context: Context
    ) {
        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .remove(KEY_FINGERPRINTS)
            .apply()
    }
    private fun fingerprint(
        qrData: String
    ): String {

        val normalized =
            qrData
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString("\n")
                .replace(
                    Regex("""\s+"""),
                    ""
                )

        val digest =
            MessageDigest
                .getInstance("SHA-256")
                .digest(
                    normalized.toByteArray(
                        Charsets.UTF_8
                    )
                )

        return digest.joinToString("") {
            "%02x".format(it)
        }
    }
}

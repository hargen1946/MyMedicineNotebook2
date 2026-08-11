package com.example.mymedicinenotebook2

enum class QrAddResult {
    ADDED,
    DUPLICATE,
    FIRST_QR_MISSING_HOSPITAL,
    EMPTY
}

class QrScanSession {

    private val qrDataList =
        mutableListOf<String>()

    /**
     * QRコードを読み取り順に追加します。
     *
     * 1枚目は、病院名を含む「51」レコードが必要です。
     * 同じQRコードは、改行や空白に差があっても重複として扱います。
     */
    fun addQrData(
        qrData: String
    ): QrAddResult {

        val normalizedData =
            normalizeQrData(qrData)

        if (normalizedData.isBlank()) {
            return QrAddResult.EMPTY
        }

        if (
            qrDataList.isEmpty() &&
            !containsHospitalRecord(normalizedData)
        ) {
            return QrAddResult.FIRST_QR_MISSING_HOSPITAL
        }

        val alreadyExists =
            qrDataList.any { savedData ->
                normalizeForComparison(savedData) ==
                        normalizeForComparison(normalizedData)
            }

        if (alreadyExists) {
            return QrAddResult.DUPLICATE
        }

        qrDataList.add(normalizedData)
        return QrAddResult.ADDED
    }

    fun getQrCount(): Int {
        return qrDataList.size
    }

    fun getAllQrData(): String {
        return qrDataList.joinToString(
            separator = "\n"
        )
    }

    fun clear() {
        qrDataList.clear()
    }

    /**
     * 病院名を示す「51」レコードが含まれているか確認します。
     */
    private fun containsHospitalRecord(
        qrData: String
    ): Boolean {

        return qrData
            .lines()
            .any { line ->
                line
                    .trimStart()
                    .startsWith("51,")
            }
    }

    /**
     * 保存時に使う形へ整えます。
     */
    private fun normalizeQrData(
        qrData: String
    ): String {

        return qrData
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .lines()
            .map { line ->
                line.trim()
            }
            .filter { line ->
                line.isNotEmpty()
            }
            .joinToString(
                separator = "\n"
            )
            .trim()
    }

    /**
     * 重複比較専用です。
     *
     * 改行、空白、タブの違いを無視して比較します。
     */
    private fun normalizeForComparison(
        qrData: String
    ): String {

        return normalizeQrData(qrData)
            .replace(
                Regex("""\s+"""),
                ""
            )
    }
}

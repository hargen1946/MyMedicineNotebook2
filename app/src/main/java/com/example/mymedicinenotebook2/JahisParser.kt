package com.example.mymedicinenotebook2

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object JahisParser {

    fun parsePrescription(qrData: String): PrescriptionRecord {

        val lines = rebuildLogicalLines(qrData)

        var prescriptionDate = ""
        var hospitalName = ""
        var department = ""
        var doctorName = ""

        val medicineMap =
            linkedMapOf<Int, MutableMedicineDetail>()

        for (line in lines) {

            val parts = line
                .split(",")
                .map { it.trim() }

            if (parts.isEmpty()) {
                continue
            }

            when (parts[0]) {

                // 処方日
                "5" -> {
                    prescriptionDate =
                        formatDate(
                            parts.getOrNull(1).orEmpty()
                        )
                }

                // 医療機関名
                "51" -> {
                    hospitalName =
                        parts.getOrNull(1)
                            .orEmpty()
                            .trim()
                }

                // 医師名・診療科
                "55" -> {
                    doctorName =
                        normalizeDisplayText(
                            parts.getOrNull(1)
                                .orEmpty()
                                .trim()
                        )

                    department =
                        normalizeDepartment(
                            parts.getOrNull(2)
                                .orEmpty()
                                .trim()
                        )
                }

                /*
                 * 薬品情報
                 *
                 * 201,RP番号,薬品名称,用量,単位名,...
                 */
                "201" -> {
                    val medicineNumber =
                        parts.getOrNull(1)
                            ?.toIntOrNull()
                            ?: continue

                    val detail =
                        medicineMap.getOrPut(
                            medicineNumber
                        ) {
                            MutableMedicineDetail()
                        }

                    val medicineName =
                        parts.getOrNull(2)
                            .orEmpty()
                            .trim()

                    if (medicineName.isNotEmpty()) {
                        detail.name =
                            normalizeDisplayText(
                                medicineName
                            )
                    }

                    val amount =
                        normalizeNumber(
                            parts.getOrNull(3)
                                .orEmpty()
                        )

                    val unit =
                        parts.getOrNull(4)
                            .orEmpty()
                            .trim()

                    if (
                        amount.isNotEmpty() &&
                        unit.isNotEmpty()
                    ) {
                        detail.medicineQuantity =
                            amount +
                                    normalizeDisplayText(unit)
                    }
                }

                /*
                 * 用法・調剤数量
                 *
                 * 301,RP番号,用法,数量,単位,...
                 */
                "301" -> {
                    val medicineNumber =
                        parts.getOrNull(1)
                            ?.toIntOrNull()
                            ?: continue

                    val detail =
                        medicineMap.getOrPut(
                            medicineNumber
                        ) {
                            MutableMedicineDetail()
                        }

                    val mainUsage =
                        normalizeDisplayText(
                            parts.getOrNull(2)
                                .orEmpty()
                                .trim()
                        )

                    if (
                        mainUsage.isNotEmpty() &&
                        mainUsage !in detail.usage
                    ) {
                        detail.usage.add(mainUsage)
                    }

                    val amount =
                        normalizeNumber(
                            parts.getOrNull(3)
                                .orEmpty()
                        )

                    val unit =
                        parts.getOrNull(4)
                            .orEmpty()
                            .trim()

                    if (
                        amount.isNotEmpty() &&
                        unit.isNotEmpty()
                    ) {
                        detail.dispensingQuantity =
                            amount +
                                    normalizeDisplayText(unit)
                    }
                }

                // 用法の補足
                "311" -> {
                    val medicineNumber =
                        parts.getOrNull(1)
                            ?.toIntOrNull()
                            ?: continue

                    val detail =
                        medicineMap.getOrPut(
                            medicineNumber
                        ) {
                            MutableMedicineDetail()
                        }

                    val usageText =
                        normalizeDisplayText(
                            parts.getOrNull(2)
                                .orEmpty()
                                .trim()
                        )

                    if (
                        usageText.isNotEmpty() &&
                        usageText !in detail.usage
                    ) {
                        detail.usage.add(usageText)
                    }
                }
            }
        }

        val medicines =
            medicineMap
                .toSortedMap()
                .values
                .mapNotNull { detail ->

                    val name =
                        detail.name.trim()

                    if (name.isEmpty()) {
                        null
                    } else {
                        MedicineDetail(
                            name = name,
                            usage =
                                detail.usage.toList(),
                            quantityInfo =
                                chooseQuantity(detail)
                        )
                    }
                }

        return PrescriptionRecord(
            prescriptionDate =
                prescriptionDate,
            hospitalName =
                hospitalName,
            department =
                department,
            doctorName =
                doctorName,
            medicines =
                medicines
        )
    }

    fun extractMedicineNames(
        qrData: String
    ): List<String> {

        return parsePrescription(qrData)
            .medicines
            .map { medicine ->
                medicine.name
            }
    }

    /*
     * 表示する数量の選び方
     *
     * ・「98日分」のような日数を優先
     * ・それ以外は薬品レコードの数量
     *   （14キット、63枚など）
     * ・薬品数量がない場合だけ調剤数量を表示
     */
    private fun chooseQuantity(
        detail: MutableMedicineDetail
    ): String {

        if (
            detail.dispensingQuantity
                .endsWith("日分")
        ) {
            return detail.dispensingQuantity
        }

        if (
            detail.medicineQuantity
                .isNotBlank()
        ) {
            return detail.medicineQuantity
        }

        return detail.dispensingQuantity
    }

    /*
     * QRコードの分割位置がレコードの途中でも、
     * 続きの文字列を前のレコードへ結合します。
     */
    private fun rebuildLogicalLines(
        qrData: String
    ): List<String> {

        if (qrData.isBlank()) {
            return emptyList()
        }

        val physicalLines =
            qrData
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }

        val logicalLines =
            mutableListOf<String>()

        for (line in physicalLines) {

            if (startsWithRecordCode(line)) {
                logicalLines.add(line)

            } else if (logicalLines.isNotEmpty()) {
                val lastIndex =
                    logicalLines.lastIndex

                logicalLines[lastIndex] =
                    logicalLines[lastIndex] +
                            line

            } else {
                logicalLines.add(line)
            }
        }

        return logicalLines
    }

    private fun startsWithRecordCode(
        line: String
    ): Boolean {

        val firstField =
            line.substringBefore(",")
                .trim()

        return firstField in setOf(
            "1",
            "2",
            "3",
            "4",
            "5",
            "11",
            "15",
            "31",
            "51",
            "55",
            "201",
            "281",
            "291",
            "301",
            "311",
            "391",
            "401",
            "411",
            "421",
            "501",
            "601",
            "701",
            "911"
        )
    }

    private fun formatDate(
        rawDate: String
    ): String {

        if (rawDate.length != 8) {
            return rawDate
        }

        return try {
            val inputFormat =
                DateTimeFormatter.ofPattern(
                    "yyyyMMdd"
                )

            val outputFormat =
                DateTimeFormatter.ofPattern(
                    "yyyy年M月d日"
                )

            LocalDate
                .parse(
                    rawDate,
                    inputFormat
                )
                .format(outputFormat)

        } catch (_: Exception) {
            rawDate
        }
    }

    /*
     * 【内科】 → 内科
     */
    private fun normalizeDepartment(
        rawValue: String
    ): String {

        return normalizeDisplayText(
            rawValue
                .removePrefix("【")
                .removeSuffix("】")
                .trim()
        )
    }

    /*
     * 環境依存文字を、表示しやすい文字へ置換します。
     *
     * 例：
     * 石﨑 淳 → 石崎 淳
     */
    private fun normalizeDisplayText(
        rawValue: String
    ): String {

        return rawValue
            .replace("﨑", "崎")
            .replace("髙", "高")
            .replace("神", "神")
            .replace("塚", "塚")
            .trim()
    }

    /*
     * 全角数字を半角数字へ統一し、
     * 14.0 のような末尾の不要なゼロも整えます。
     */
    private fun normalizeNumber(
        rawValue: String
    ): String {

        val normalized =
            buildString {
                for (char in rawValue.trim()) {
                    append(
                        when (char) {
                            in '０'..'９' ->
                                (
                                        '0'.code +
                                                char.code -
                                                '０'.code
                                        ).toChar()

                            '．' -> '.'
                            else -> char
                        }
                    )
                }
            }

        val number =
            normalized.toDoubleOrNull()
                ?: return normalized

        return if (
            number % 1.0 == 0.0
        ) {
            number.toLong()
                .toString()
        } else {
            normalized
                .trimEnd('0')
                .trimEnd('.')
        }
    }

    private data class MutableMedicineDetail(
        var name: String = "",
        val usage: MutableList<String> =
            mutableListOf(),
        var medicineQuantity: String = "",
        var dispensingQuantity: String = ""
    )
}

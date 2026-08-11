package com.example.mymedicinenotebook2

object JahisDebugFormatter {

    fun format(qrData: String): String {

        if (qrData.isBlank()) {
            return "解析するQRデータがありません。"
        }

        val lines = qrData
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .lines()
            .map { line -> line.trim() }
            .filter { line -> line.isNotEmpty() }

        return lines
            .mapIndexed { index, line ->

                val recordCode =
                    line.substringBefore(",")
                        .trim()

                val description =
                    recordDescription(recordCode)

                buildString {
                    append(index + 1)
                    append(". ")

                    if (description.isNotEmpty()) {
                        append("[")
                        append(recordCode)
                        append("：")
                        append(description)
                        append("]")
                    } else {
                        append("[")
                        append(recordCode)
                        append("]")
                    }

                    append("\n")
                    append(line)
                }
            }
            .joinToString(
                separator = "\n\n"
            )
    }

    private fun recordDescription(
        recordCode: String
    ): String {

        return when (recordCode) {
            "1" -> "JAHIS基本情報"
            "5" -> "処方日など"
            "11" -> "患者情報"
            "51" -> "医療機関情報"
            "55" -> "診療科など"
            "201" -> "薬品情報"
            "301" -> "用法・調剤単位"
            "311" -> "用法の補足"
            "501" -> "コメント"
            else -> "未分類レコード"
        }
    }
}
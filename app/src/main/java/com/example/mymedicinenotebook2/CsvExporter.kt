package com.example.mymedicinenotebook2

object CsvExporter {

    fun createCsv(
        records: List<PrescriptionRecord>
    ): String {

        val lines =
            mutableListOf<String>()

        lines.add(
            listOf(
                "処方日",
                "病院名",
                "診療科",
                "医師名",
                "薬品名",
                "用法",
                "数量"
            ).joinToString(",")
        )

        records.forEach { record ->

            record.medicines.forEach { medicine ->

                lines.add(
                    listOf(
                        record.prescriptionDate,
                        record.hospitalName,
                        record.department,
                        record.doctorName,
                        medicine.name,
                        medicine.usage.joinToString(" / "),
                        medicine.quantityInfo
                    )
                        .joinToString(",") {
                            escapeCsv(it)
                        }
                )
            }
        }

        return lines.joinToString(
            separator = "\r\n"
        )
    }

    private fun escapeCsv(
        value: String
    ): String {

        val escaped =
            value.replace(
                "\"",
                "\"\""
            )

        return "\"$escaped\""
    }
}
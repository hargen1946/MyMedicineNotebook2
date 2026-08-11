package com.example.mymedicinenotebook2

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object PrescriptionStorage {

    private const val PREFERENCES_NAME =
        "com.example.mymedicinenotebook2.PRESCRIPTION_RECORDS"

    private const val KEY_RECORDS =
        "prescription_records"

    fun saveRecord(
        context: Context,
        record: PrescriptionRecord
    ): Boolean {

        val records =
            loadRecords(context)
                .toMutableList()

        val sameIndex =
            records.indexOfFirst { saved ->
                isSamePrescription(saved, record)
            }

        if (sameIndex >= 0) {
            val current = records[sameIndex]
            val better = chooseMoreComplete(current, record)

            if (better == current) {
                return false
            }

            records[sameIndex] = better
            writeRecords(context, records)
            return true
        }

        records.add(0, record)
        writeRecords(context, records)
        return true
    }

    fun loadRecords(
        context: Context
    ): List<PrescriptionRecord> {

        val preferences =
            context.getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )

        val jsonText =
            preferences.getString(
                KEY_RECORDS,
                "[]"
            ) ?: "[]"

        val loadedRecords =
            try {
                val jsonArray =
                    JSONArray(jsonText)

                buildList {
                    for (
                    index in 0 until jsonArray.length()
                    ) {
                        val jsonObject =
                            jsonArray.optJSONObject(index)
                                ?: continue

                        add(
                            jsonToRecord(jsonObject)
                        )
                    }
                }

            } catch (_: Exception) {
                emptyList()
            }

        val cleanedRecords =
            mergeDuplicatePrescriptions(loadedRecords)

        if (cleanedRecords != loadedRecords) {
            writeRecords(context, cleanedRecords)
        }

        return cleanedRecords
    }

    /**
     * 指定した1件だけを削除します。
     */
    fun deleteRecord(
        context: Context,
        record: PrescriptionRecord
    ): Boolean {

        val records =
            loadRecords(context)
                .toMutableList()

        val removed =
            records.remove(record)

        if (removed) {
            writeRecords(
                context = context,
                records = records
            )
        }

        return removed
    }

    fun clearAll(
        context: Context
    ) {
        context
            .getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .remove(KEY_RECORDS)
            .apply()
    }

    private fun mergeDuplicatePrescriptions(
        records: List<PrescriptionRecord>
    ): List<PrescriptionRecord> {

        val merged = mutableListOf<PrescriptionRecord>()

        records.forEach { record ->
            val index =
                merged.indexOfFirst { saved ->
                    isSamePrescription(saved, record)
                }

            if (index < 0) {
                merged.add(record)
            } else {
                merged[index] =
                    chooseMoreComplete(merged[index], record)
            }
        }

        return merged
    }

    private fun isSamePrescription(
        first: PrescriptionRecord,
        second: PrescriptionRecord
    ): Boolean {

        return normalizeText(first.prescriptionDate) ==
                normalizeText(second.prescriptionDate) &&
                normalizeText(first.hospitalName) ==
                normalizeText(second.hospitalName)
    }

    private fun chooseMoreComplete(
        first: PrescriptionRecord,
        second: PrescriptionRecord
    ): PrescriptionRecord {

        return if (completenessScore(second) >
            completenessScore(first)
        ) {
            second
        } else {
            first
        }
    }

    private fun completenessScore(
        record: PrescriptionRecord
    ): Int {

        var score = 0

        if (record.prescriptionDate.isNotBlank()) score += 10
        if (record.hospitalName.isNotBlank()) score += 10
        if (record.department.isNotBlank()) score += 3
        if (record.doctorName.isNotBlank()) score += 3

        score += record.medicines.size * 20

        record.medicines.forEach { medicine ->
            if (medicine.name.isNotBlank()) score += 5
            score += medicine.usage.count { it.isNotBlank() } * 2
            if (medicine.quantityInfo.isNotBlank()) score += 3
        }

        return score
    }

    private fun normalizeText(
        text: String
    ): String {

        return text
            .replace(Regex("""\s+"""), "")
            .trim()
    }

    private fun writeRecords(
        context: Context,
        records: List<PrescriptionRecord>
    ) {

        val jsonArray =
            JSONArray()

        records.forEach { record ->
            jsonArray.put(
                recordToJson(record)
            )
        }

        context
            .getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                KEY_RECORDS,
                jsonArray.toString()
            )
            .apply()
    }

    private fun recordToJson(
        record: PrescriptionRecord
    ): JSONObject {

        val medicineArray =
            JSONArray()

        record.medicines.forEach { medicine ->

            val usageArray =
                JSONArray()

            medicine.usage.forEach { usage ->
                usageArray.put(usage)
            }

            medicineArray.put(
                JSONObject()
                    .put(
                        "name",
                        medicine.name
                    )
                    .put(
                        "usage",
                        usageArray
                    )
                    .put(
                        "quantityInfo",
                        medicine.quantityInfo
                    )
            )
        }

        return JSONObject()
            .put(
                "prescriptionDate",
                record.prescriptionDate
            )
            .put(
                "hospitalName",
                record.hospitalName
            )
            .put(
                "department",
                record.department
            )
            .put(
                "doctorName",
                record.doctorName
            )
            .put(
                "medicines",
                medicineArray
            )
    }

    private fun jsonToRecord(
        jsonObject: JSONObject
    ): PrescriptionRecord {

        val medicineArray =
            jsonObject.optJSONArray(
                "medicines"
            ) ?: JSONArray()

        val medicines =
            buildList {

                for (
                index in 0 until medicineArray.length()
                ) {
                    val medicineObject =
                        medicineArray.optJSONObject(index)
                            ?: continue

                    val usageArray =
                        medicineObject.optJSONArray(
                            "usage"
                        ) ?: JSONArray()

                    val usage =
                        buildList {
                            for (
                            usageIndex in
                            0 until usageArray.length()
                            ) {
                                val usageText =
                                    usageArray.optString(
                                        usageIndex
                                    )

                                if (
                                    usageText.isNotBlank()
                                ) {
                                    add(usageText)
                                }
                            }
                        }

                    add(
                        MedicineDetail(
                            name =
                                medicineObject.optString(
                                    "name"
                                ),
                            usage = usage,
                            quantityInfo =
                                medicineObject.optString(
                                    "quantityInfo"
                                )
                        )
                    )
                }
            }

        return PrescriptionRecord(
            prescriptionDate =
                jsonObject.optString(
                    "prescriptionDate"
                ),
            hospitalName =
                jsonObject.optString(
                    "hospitalName"
                ),
            department =
                jsonObject.optString(
                    "department"
                ),
            doctorName =
                jsonObject.optString(
                    "doctorName"
                ),
            medicines = medicines
        )
    }
}

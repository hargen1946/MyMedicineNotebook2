package com.example.mymedicinenotebook2

data class PrescriptionRecord(
    val prescriptionDate: String,
    val hospitalName: String,
    val department: String,
    val doctorName: String,
    val medicines: List<MedicineDetail>
)

data class MedicineDetail(
    val name: String,
    val usage: List<String>,
    val quantityInfo: String
)

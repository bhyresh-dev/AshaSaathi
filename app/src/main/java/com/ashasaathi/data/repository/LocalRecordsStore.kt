package com.ashasaathi.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

data class LocalVaccineRecord(
    val id: String = UUID.randomUUID().toString(),
    val childName: String,
    val vaccineName: String,
    val motherName: String,
    val village: String,
    val date: String
)

data class LocalDotsRecord(
    val id: String = UUID.randomUUID().toString(),
    val patientName: String,
    val nikshayId: String,
    val dotsTaken: Boolean,
    val sideEffects: String,
    val date: String
)

object LocalRecordsStore {
    private val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val _vaccines = MutableStateFlow<List<LocalVaccineRecord>>(emptyList())
    val vaccines = _vaccines.asStateFlow()

    private val _dots = MutableStateFlow<List<LocalDotsRecord>>(emptyList())
    val dots = _dots.asStateFlow()

    fun addVaccine(record: LocalVaccineRecord) {
        _vaccines.value = _vaccines.value + record
    }

    fun addDots(record: LocalDotsRecord) {
        _dots.value = _dots.value + record
    }

    fun today(): String = fmt.format(Date())
}

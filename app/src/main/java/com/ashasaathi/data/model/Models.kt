package com.ashasaathi.data.model

import com.google.firebase.Timestamp

enum class RiskLevel { GREEN, YELLOW, RED }
enum class SyncStatus { SYNCED, PENDING, CONFLICT }
enum class Gender { M, F, OTHER }
enum class VisitType { ANC, PNC, IMMUNISATION, TB_DOTS, ELDERLY, FAMILY_PLANNING, GENERAL }
enum class Language { hi, en, kn }

data class GeoPoint(val latitude: Double = 0.0, val longitude: Double = 0.0)

data class Worker(
    val workerId: String = "",
    val name: String = "",
    val phone: String = "",
    val rchPortalId: String = "",
    val phcId: String = "",
    val phcName: String = "",
    val subCentre: String = "",
    val village: String = "",
    val block: String = "",
    val district: String = "",
    val state: String = "",
    val villageGeoCenter: GeoPoint? = null,
    val languagePreference: String = "hi",
    val fcmToken: String? = null,
    val isActive: Boolean = true,
    val profilePhotoUrl: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val syncStatus: String = "SYNCED",
    val createdBy: String = ""
)

data class Household(
    val householdId: String = "",
    val workerId: String = "",
    val phcId: String = "",
    val subCentre: String = "",
    val houseNumber: String = "",
    val village: String = "",
    val headOfFamily: String = "",
    val totalMembers: Int = 0,
    val eligibleCouples: Int = 0,
    val pregnantWomenCount: Int = 0,
    val childrenUnder5Count: Int = 0,
    val chronicConditions: List<String> = emptyList(),
    val location: GeoPoint = GeoPoint(),
    val overallRiskLevel: String = "GREEN",
    val lastVisitDate: String? = null,
    val memberIds: List<String> = emptyList(),
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val syncStatus: String = "PENDING",
    val createdBy: String = ""
)

data class Patient(
    val patientId: String = "",
    val householdId: String = "",
    val workerId: String = "",
    val phcId: String = "",
    val name: String = "",
    val rchMctsId: String? = null,
    val aadhaarMasked: String? = null,
    val dob: String = "",
    val age: Int? = null,
    val gender: String = "F",
    val phone: String? = null,
    val bloodGroup: String? = null,
    val location: GeoPoint = GeoPoint(),
    val isPregnant: Boolean = false,
    val lmpDate: String? = null,
    val edd: String? = null,
    val gestationalAgeWeeks: Int? = null,
    val trimester: Int? = null,
    val isChildUnder5: Boolean = false,
    val motherPatientId: String? = null,
    val hasTB: Boolean = false,
    val isElderly: Boolean = false,
    val currentRiskLevel: String = "GREEN",
    val riskFlags: List<String> = emptyList(),
    val lastVisitDate: String? = null,
    val lastVisitId: String? = null,
    val isActive: Boolean = true,
    val village: String? = null,
    val chronicConditions: List<String> = emptyList(),
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val syncStatus: String = "PENDING",
    val createdBy: String = ""
)

data class VitalSigns(
    val bpSystolic: Int? = null,
    val bpDiastolic: Int? = null,
    val weightKg: Double? = null,
    val heightCm: Double? = null,
    val muacCm: Double? = null,
    val hemoglobinGdL: Double? = null,
    val fetalHeartRateBpm: Int? = null,
    val fundalHeightCm: Double? = null,
    val temperatureCelsius: Double? = null,
    val spo2Percent: Int? = null,
    val pulseRate: Int? = null
)

data class Visit(
    val visitId: String = "",
    val patientId: String = "",
    val householdId: String = "",
    val workerId: String = "",
    val phcId: String = "",
    val visitType: String = "GENERAL",
    val visitDate: String = "",
    val vitals: VitalSigns = VitalSigns(),
    val hasFever: Boolean = false,
    val coughDays: Int? = null,
    val complaints: List<String> = emptyList(),
    val clinicalNotes: String = "",
    val voiceTranscript: String? = null,
    val riskLevel: String = "GREEN",
    val riskFlags: List<String> = emptyList(),
    val aiClassifierScore: Double? = null,
    val referralNeeded: Boolean = false,
    val referralNote: String? = null,
    val referralFacility: String? = null,
    val photoUrls: List<String> = emptyList(),
    val location: GeoPoint = GeoPoint(),
    val nhmActivityCode: String? = null,
    val incentiveAmountRs: Double? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val syncStatus: String = "PENDING",
    val createdBy: String = ""
)

data class VaccinationRecord(
    val patientId: String = "",
    val workerId: String = "",
    val vaccineId: String = "",
    val vaccineName: String = "",
    val dose: String = "",
    val scheduledDate: String = "",
    val administeredDate: String? = null,
    val status: String = "PENDING",
    val batchNumber: String? = null,
    val site: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val syncStatus: String = "PENDING",
    val createdBy: String = ""
)

data class TBPatient(
    val tbPatientId: String = "",
    val patientId: String = "",
    val workerId: String = "",
    val nikshayId: String = "",
    val diagnosisDate: String = "",
    val treatmentStartDate: String = "",
    val category: String = "",
    val regime: String = "",
    val dotsDue: Boolean = false,
    val lastDotsDate: String? = null,
    val adherencePercent: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val syncStatus: String = "PENDING",
    val createdBy: String = ""
)

data class DiaryEntry(
    val entryId: String = "",
    val workerId: String = "",
    val date: String = "",
    val content: String = "",
    val voiceTranscript: String? = null,
    val mood: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val syncStatus: String = "PENDING",
    val createdBy: String = ""
)

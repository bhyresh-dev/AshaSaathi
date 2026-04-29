package com.ashasaathi.data.model

import com.google.firebase.Timestamp

// ── Enums ─────────────────────────────────────────────────────────────────────

enum class RiskLevel { GREEN, YELLOW, RED }
enum class SyncStatus { SYNCED, PENDING, CONFLICT }
enum class Gender { M, F, OTHER }
enum class Language(val code: String, val displayName: String) {
    hi("hi", "हिंदी"),
    kn("kn", "ಕನ್ನಡ"),
    en("en", "English")
}
enum class VisitType {
    ANC, PNC, IMMUNISATION, TB_DOTS, ELDERLY, FAMILY_PLANNING, HBYC, GENERAL
}
enum class TTDose { NONE, TT1, TT2, BOOSTER }
enum class DeliveryType { NORMAL, CAESAREAN, ASSISTED }
enum class DeliveryPlace { HOME, SC, PHC, CHC, DH, PRIVATE }
enum class UrineResult { NIL, TRACE, PLUS1, PLUS2, PLUS3 }

// ── Geo ───────────────────────────────────────────────────────────────────────

data class GeoPoint(val latitude: Double = 0.0, val longitude: Double = 0.0)

// ── Worker ────────────────────────────────────────────────────────────────────

data class Worker(
    val workerId: String = "",
    val name: String = "",
    val phone: String = "",
    val rchPortalId: String = "",
    val aadhaarMasked: String? = null,
    val phcId: String = "",
    val phcName: String = "",
    val subCentre: String = "",
    val village: String = "",
    val block: String = "",
    val district: String = "",
    val state: String = "",
    val bankAccountNo: String? = null,
    val ifscCode: String? = null,
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

// ── Household ─────────────────────────────────────────────────────────────────

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
    val elderlyCount: Int = 0,
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

// ── Patient ───────────────────────────────────────────────────────────────────

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

    // Pregnancy
    val isPregnant: Boolean = false,
    val lmpDate: String? = null,
    val edd: String? = null,
    val gestationalAgeWeeks: Int? = null,
    val trimester: Int? = null,
    val ancCount: Int = 0,
    val ifaCumulativeCount: Int = 0,       // running total toward 180
    val ttDoseStatus: String = "NONE",
    val fastingGlucose: Float? = null,
    val ogtt2hr: Float? = null,
    val jsyInstallments: Map<String, Boolean> = emptyMap(),
    val pmmvyInstallments: Map<String, Boolean> = emptyMap(),

    // Delivery
    val deliveryDate: String? = null,
    val deliveryType: String? = null,
    val deliveryPlace: String? = null,
    val birthWeight: Float? = null,
    val neonatalStatus: String? = null,

    // Child
    val isChildUnder5: Boolean = false,
    val motherPatientId: String? = null,
    val ficStatus: Boolean = false,        // Fully Immunised Child
    val cicStatus: Boolean = false,        // Completely Immunised Child

    // TB
    val hasTB: Boolean = false,
    val nikshayId: String? = null,
    val dotsRegimen: String? = null,
    val dotsStartDate: String? = null,

    // General
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

// ── Vitals ────────────────────────────────────────────────────────────────────

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
    val pulseRate: Int? = null,
    // Pregnancy-specific
    val ifaTabletsToday: Int? = null,
    val ttDoseGiven: String? = null,
    val fastingGlucose: Double? = null,
    val ogtt2hr: Double? = null,
    val urineProtein: String? = null,
    val urineSugar: String? = null,
    val fundalHeight: Double? = null
)

// ── Visit ─────────────────────────────────────────────────────────────────────

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

// ── Vaccination ───────────────────────────────────────────────────────────────

data class VaccinationRecord(
    val recordId: String = "",
    val patientId: String = "",
    val workerId: String = "",
    val vaccineId: String = "",
    val vaccineName: String = "",
    val dose: String = "",
    val scheduledDate: String = "",
    val administeredDate: String? = null,
    val status: String = "PENDING",        // PENDING, ADMINISTERED, MISSED, UPCOMING
    val batchNumber: String? = null,
    val site: String? = null,              // Left Arm, Right Thigh, etc.
    val givenBy: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val syncStatus: String = "PENDING",
    val createdBy: String = ""
)

// ── UIP Vaccine Schedule ──────────────────────────────────────────────────────

data class UIPVaccine(
    val id: String,
    val nameHi: String,
    val nameEn: String,
    val dose: String,
    val ageWindowDays: IntRange,      // acceptable window in days from DOB
    val targetAgeDays: Int,           // ideal day
    val site: String,
    val route: String,
    val isRoutine: Boolean = true
)

object UIPSchedule {
    val vaccines: List<UIPVaccine> = listOf(
        UIPVaccine("bcg",      "बीसीजी",          "BCG",          "Single",  0..15,     0,    "Left Upper Arm","ID"),
        UIPVaccine("opv0",     "ओपीवी 0",          "OPV 0",        "Birth",   0..15,     0,    "Oral",         "Oral"),
        UIPVaccine("hepb0",    "हेपB 0",            "Hep B 0",      "Birth",   0..24,     0,    "Right Thigh",  "IM"),
        UIPVaccine("opv1",     "ओपीवी 1",          "OPV 1",        "1",       35..70,    42,   "Oral",         "Oral"),
        UIPVaccine("penta1",   "पेंटावेलेंट 1",    "Penta 1",      "1",       35..70,    42,   "Left Thigh",   "IM"),
        UIPVaccine("pcv1",     "पीसीवी 1",          "PCV 1",        "1",       35..70,    42,   "Left Thigh",   "IM"),
        UIPVaccine("rota1",    "रोटावायरस 1",      "Rotavirus 1",  "1",       35..70,    42,   "Oral",         "Oral"),
        UIPVaccine("ipv1",     "आईपीवी 1",          "IPV 1",        "1",       35..70,    42,   "Right Thigh",  "IM"),
        UIPVaccine("opv2",     "ओपीवी 2",          "OPV 2",        "2",       63..98,    70,   "Oral",         "Oral"),
        UIPVaccine("penta2",   "पेंटावेलेंट 2",    "Penta 2",      "2",       63..98,    70,   "Left Thigh",   "IM"),
        UIPVaccine("pcv2",     "पीसीवी 2",          "PCV 2",        "2",       63..98,    70,   "Left Thigh",   "IM"),
        UIPVaccine("rota2",    "रोटावायरस 2",      "Rotavirus 2",  "2",       63..98,    70,   "Oral",         "Oral"),
        UIPVaccine("opv3",     "ओपीवी 3",          "OPV 3",        "3",       91..126,   98,   "Oral",         "Oral"),
        UIPVaccine("penta3",   "पेंटावेलेंट 3",    "Penta 3",      "3",       91..126,   98,   "Left Thigh",   "IM"),
        UIPVaccine("pcv3",     "पीसीवी 3",          "PCV 3 Booster",  "3",    91..126,   98,   "Left Thigh",   "IM"),
        UIPVaccine("rota3",    "रोटावायरस 3",      "Rotavirus 3",  "3",       91..126,   98,   "Oral",         "Oral"),
        UIPVaccine("ipv2",     "आईपीवी 2",          "IPV 2",        "2",       91..126,   98,   "Right Thigh",  "IM"),
        UIPVaccine("mr1",      "एमआर 1",            "MR 1",         "1",       270..365,  274,  "Right Upper Arm","SC"),
        UIPVaccine("vita1",    "विटामिन ए 1",       "Vitamin A 1",  "1",       270..365,  274,  "Oral",         "Oral"),
        UIPVaccine("je1",      "जेई 1",             "JE 1",         "1",       270..365,  274,  "Left Upper Arm","SC"),
        UIPVaccine("dptb1",    "डीपीटी बूस्टर 1",  "DPT Booster 1","B1",      487..730,  548,  "Left Thigh",   "IM"),
        UIPVaccine("opvb",     "ओपीवी बूस्टर",     "OPV Booster",  "Booster", 487..730,  548,  "Oral",         "Oral"),
        UIPVaccine("mr2",      "एमआर 2",            "MR 2",         "2",       487..730,  548,  "Right Upper Arm","SC"),
        UIPVaccine("je2",      "जेई 2",             "JE 2",         "2",       487..730,  548,  "Left Upper Arm","SC"),
        UIPVaccine("vita2",    "विटामिन ए 2",       "Vitamin A 2",  "2",       487..730,  548,  "Oral",         "Oral"),
        UIPVaccine("vita3",    "विटामिन ए 3",       "Vitamin A 3",  "3",       547..912,  730,  "Oral",         "Oral"),
        UIPVaccine("vita4",    "विटामिन ए 4",       "Vitamin A 4",  "4",       730..1095, 912,  "Oral",         "Oral"),
        UIPVaccine("vita5",    "विटामिन ए 5",       "Vitamin A 5",  "5",       912..1277, 1095, "Oral",         "Oral"),
        UIPVaccine("dptb2",    "डीपीटी बूस्टर 2",  "DPT Booster 2","B2",      1825..2190,2007, "Left Upper Arm","IM"),
        UIPVaccine("vita6",    "विटामिन ए 6",       "Vitamin A 6",  "6",       1095..1460,1277, "Oral",         "Oral"),
        UIPVaccine("vita7",    "विटामिन ए 7",       "Vitamin A 7",  "7",       1277..1642,1460, "Oral",         "Oral"),
        UIPVaccine("vita8",    "विटामिन ए 8",       "Vitamin A 8",  "8",       1460..1825,1642, "Oral",         "Oral"),
        UIPVaccine("vita9",    "विटामिन ए 9",       "Vitamin A 9",  "9",       1642..2007,1825, "Oral",         "Oral"),
    )
}

// ── TB ────────────────────────────────────────────────────────────────────────

data class TBPatient(
    val tbPatientId: String = "",
    val patientId: String = "",
    val workerId: String = "",
    val nikshayId: String = "",
    val diagnosisDate: String = "",
    val treatmentStartDate: String = "",
    val category: String = "",
    val regime: String = "",
    val dotsCalendar: Map<String, String> = emptyMap(), // date → "TAKEN"|"MISSED"|reason
    val adherencePercent: Int = 0,
    val dbtReceivedMonths: List<String> = emptyList(),  // months where 500 Rs received
    val isActive: Boolean = true,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val syncStatus: String = "PENDING",
    val createdBy: String = ""
)

// ── Diary ─────────────────────────────────────────────────────────────────────

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

// ── Risk ──────────────────────────────────────────────────────────────────────

data class RiskFlag(
    val code: String,
    val severity: String,   // RED | YELLOW | GREEN
    val reasonHi: String,
    val reasonEn: String
)

data class RiskResult(
    val level: String,
    val flags: List<RiskFlag>,
    val aiScore: Float = 0f
)

// ── HMIS / Incentive codes ────────────────────────────────────────────────────

object NHMActivityCodes {
    val codes = mapOf(
        "ANC_REG"        to Triple("ANC पंजीकरण",            "ANC Registration",       250.0),
        "ANC1"           to Triple("पहला ANC",                 "1st ANC Visit",          0.0),
        "ANC2"           to Triple("दूसरा ANC",                "2nd ANC Visit",          0.0),
        "ANC3"           to Triple("तीसरा ANC",                "3rd ANC Visit",          0.0),
        "ANC4"           to Triple("चौथा ANC",                 "4th ANC Visit",          0.0),
        "DELIVERY_INST"  to Triple("संस्थागत प्रसव",           "Institutional Delivery", 600.0),
        "DELIVERY_HOME"  to Triple("गृह प्रसव",                "Home Delivery",          300.0),
        "PNC1"           to Triple("पहला PNC",                  "1st PNC Visit",          250.0),
        "IMMUNISATION"   to Triple("टीकाकरण",                  "Immunisation Visit",     150.0),
        "DOTS_DAILY"     to Triple("DOTS दैनिक",               "TB DOTS Daily",          0.0),
        "TB_REFERRED"    to Triple("TB रेफरल",                  "TB Referral",            500.0),
        "HIGH_RISK_REF"  to Triple("उच्च जोखिम रेफरल",        "High Risk Referral",     250.0),
        "JSY_RURAL"      to Triple("JSY ग्रामीण",              "JSY Rural",              600.0),
    )
}

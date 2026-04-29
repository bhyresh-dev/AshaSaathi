package com.ashasaathi.data.model

enum class VoiceFormType(
    val titleHi: String, val titleKn: String, val titleEn: String,
    val emoji: String,
    val promptHi: String, val promptKn: String, val promptEn: String
) {
    HOUSEHOLD(
        titleHi = "परिवार पंजीकरण",
        titleKn = "ಕುಟುಂಬ ನೋಂದಣಿ",
        titleEn = "Household Registration",
        emoji   = "🏠",
        promptHi = "नंबर देकर बोलें:\n1. घर नंबर\n2. मुखिया का नाम\n3. गाँव का नाम\n4. कुल सदस्य\n5. योग्य दंपति\n6. गर्भवती महिलाएं\n7. 5 साल से कम बच्चे\n8. बुजुर्ग (60+)",
        promptKn = "ಅಂಕಿ ಹೇಳಿ:\n1. ಮನೆ ನಂಬರ್\n2. ಮನೆ ಯಜಮಾನರ ಹೆಸರು\n3. ಗ್ರಾಮ\n4. ಒಟ್ಟು ಸದಸ್ಯರು\n5. ಅರ್ಹ ದಂಪತಿ\n6. ಗರ್ಭಿಣಿ ಮಹಿಳೆಯರು\n7. 5 ವರ್ಷದೊಳಗಿನ ಮಕ್ಕಳು\n8. ಹಿರಿಯ (60+)",
        promptEn = "Speak in numbered points:\n1. House number\n2. Head of family name\n3. Village\n4. Total members\n5. Eligible couples\n6. Pregnant women\n7. Children under 5\n8. Elderly (60+)"
    ),
    PATIENT(
        titleHi = "लाभार्थी पंजीकरण",
        titleKn = "ಫಲಾನುಭವಿ ನೋಂದಣಿ",
        titleEn = "Beneficiary Registration",
        emoji   = "👤",
        promptHi = "नंबर देकर बोलें:\n1. महिला का नाम\n2. पति का नाम\n3. उम्र (वर्ष में)\n4. फोन नंबर\n5. गाँव\n6. RCH आईडी (यदि है)",
        promptKn = "ಅಂಕಿ ಹೇಳಿ:\n1. ಮಹಿಳೆಯ ಹೆಸರು\n2. ಪತಿಯ ಹೆಸರು\n3. ವಯಸ್ಸು\n4. ಫೋನ್ ಸಂಖ್ಯೆ\n5. ಗ್ರಾಮ\n6. RCH ಐಡಿ",
        promptEn = "Speak in numbered points:\n1. Patient name\n2. Husband name\n3. Age in years\n4. Phone number\n5. Village\n6. RCH ID if available"
    ),
    ANC_VISIT(
        titleHi = "ANC विजिट (गर्भवती)",
        titleKn = "ANC ಭೇಟಿ (ಗರ್ಭಿಣಿ)",
        titleEn = "ANC Visit (Pregnancy)",
        emoji   = "🤰",
        promptHi = "नंबर देकर बोलें:\n1. महिला का नाम\n2. अंतिम माहवारी तारीख\n3. रक्तचाप (जैसे 120 बाय 80)\n4. वजन किलो में\n5. खून (Hb) g/dL में\n6. IFA गोलियां (संख्या)\n7. TT डोज (TT1/TT2/Booster)\n8. पेशाब प्रोटीन\n9. बुखार है या नहीं\n10. कोई समस्या",
        promptKn = "ಅಂಕಿ ಹೇಳಿ:\n1. ಮಹಿಳೆ ಹೆಸರು\n2. LMP ದಿನಾಂಕ\n3. ರಕ್ತದೊತ್ತಡ\n4. ತೂಕ (kg)\n5. Hb (g/dL)\n6. IFA ಮಾತ್ರೆ ಸಂಖ್ಯೆ\n7. TT ಡೋಸ್\n8. ಮೂತ್ರ ಪ್ರೋಟೀನ್\n9. ಜ್ವರ ಇದೆಯೇ\n10. ಯಾವುದಾದರೂ ಸಮಸ್ಯೆ",
        promptEn = "Speak in numbered points:\n1. Patient name\n2. LMP date\n3. Blood pressure (e.g. 120 by 80)\n4. Weight in kg\n5. Hb in g/dL\n6. IFA tablets count\n7. TT dose (TT1/TT2/Booster)\n8. Urine protein result\n9. Fever yes or no\n10. Any complaints"
    ),
    VACCINE(
        titleHi = "टीकाकरण दर्ज करें",
        titleKn = "ಲಸಿಕೆ ದಾಖಲು",
        titleEn = "Record Vaccination",
        emoji   = "💉",
        promptHi = "नंबर देकर बोलें:\n1. बच्चे का नाम\n2. जन्म तिथि\n3. टीके का नाम\n4. माँ का नाम\n5. गाँव",
        promptKn = "ಅಂಕಿ ಹೇಳಿ:\n1. ಮಗುವಿನ ಹೆಸರು\n2. ಹುಟ್ಟಿದ ದಿನಾಂಕ\n3. ಲಸಿಕೆ ಹೆಸರು\n4. ತಾಯಿ ಹೆಸರು\n5. ಗ್ರಾಮ",
        promptEn = "Speak in numbered points:\n1. Child name\n2. Date of birth\n3. Vaccine name\n4. Mother name\n5. Village"
    ),
    TB_DOTS(
        titleHi = "TB DOTS दर्ज करें",
        titleKn = "TB DOTS ದಾಖಲು",
        titleEn = "Record TB DOTS",
        emoji   = "💊",
        promptHi = "नंबर देकर बोलें:\n1. मरीज का नाम\n2. निक्षय आईडी\n3. आज DOTS लिया या नहीं (हाँ/नहीं)\n4. कोई दुष्प्रभाव (यदि हो)",
        promptKn = "ಅಂಕಿ ಹೇಳಿ:\n1. ರೋಗಿಯ ಹೆಸರು\n2. ನಿಕ್ಷಯ್ ಐಡಿ\n3. ಇಂದು DOTS ತೆಗೆದುಕೊಂಡರೇ (ಹೌದು/ಇಲ್ಲ)\n4. ಯಾವುದಾದರೂ ಅಡ್ಡ ಪರಿಣಾಮ",
        promptEn = "Speak in numbered points:\n1. Patient name\n2. Nikshay ID\n3. DOTS taken today (yes/no)\n4. Any side effects"
    )
}

data class ExtractedHousehold(
    val houseNumber: String      = "",
    val headOfFamily: String     = "",
    val village: String          = "",
    val totalMembers: Int?       = null,
    val eligibleCouples: Int?    = null,
    val pregnantWomen: Int?      = null,
    val childrenUnder5: Int?     = null,
    val elderly: Int?            = null,
    val chronicConditions: List<String> = emptyList()
)

data class ExtractedPatient(
    val name: String         = "",
    val husbandName: String  = "",
    val age: Int?            = null,
    val phone: String        = "",
    val village: String      = "",
    val rchId: String        = "",
    val isPregnant: Boolean  = false,
    val lmpDate: String      = ""
)

data class ExtractedANC(
    val patientName: String   = "",
    val lmpDate: String       = "",
    val bpSystolic: Int?      = null,
    val bpDiastolic: Int?     = null,
    val weightKg: Double?     = null,
    val hemoglobinGdL: Double? = null,
    val ifaTabletsGiven: Int? = null,
    val ttDose: String        = "",
    val urineProtein: String  = "",
    val fastingGlucose: Double? = null,
    val hasFever: Boolean     = false,
    val complaints: String    = ""
)

data class ExtractedVaccine(
    val childName: String  = "",
    val dob: String        = "",
    val vaccineName: String= "",
    val motherName: String = "",
    val village: String    = ""
)

data class ExtractedDOTS(
    val patientName: String = "",
    val nikshayId: String   = "",
    val dotsTaken: Boolean  = false,
    val sideEffects: String = ""
)

package com.ashasaathi.ui.navigation

object Route {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val PROFILE_SETUP = "profile_setup"
    const val HOME = "home"
    const val HOUSEHOLDS = "households"
    const val HOUSEHOLD_DETAIL = "household_detail/{householdId}"
    const val ADD_HOUSEHOLD = "add_household"
    const val PATIENT_DETAIL = "patient_detail/{patientId}"
    const val VISIT_FORM = "visit_form/{patientId}"
    const val VACCINATION = "vaccination"
    const val TB_DOTS = "tb_dots"
    const val DIARY = "diary"
    const val MCP_CARD = "mcp_card/{patientId}"
    const val PLANNER = "planner"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
    const val MODEL_SETUP = "model_setup"
    const val VOICE_FORM = "voice_form"
    const val MAP = "map"

    fun householdDetail(id: String) = "household_detail/$id"
    fun patientDetail(id: String) = "patient_detail/$id"
    fun visitForm(patientId: String) = "visit_form/$patientId"
    fun mcpCard(patientId: String) = "mcp_card/$patientId"
}

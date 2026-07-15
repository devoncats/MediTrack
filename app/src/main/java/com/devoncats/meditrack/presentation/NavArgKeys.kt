package com.devoncats.meditrack.presentation

/**
 * Fragment argument / bundle keys shared between the writer (bundleOf/putExtra call sites,
 * NavDeepLinkBuilder) and the reader (Fragment.arguments) side of each navigation hop. Centralized
 * here instead of repeated as raw strings so a typo on either side fails to compile rather than
 * silently reading a null/default argument at runtime.
 *
 * These must match the corresponding `<argument android:name="...">` declarations in the nav
 * graphs (auth_graph.xml, patient_graph.xml, caregiver_graph.xml, senior_graph.xml).
 */
object NavArgKeys {
    const val MEDICATION_ID = "medicationId"
    const val SCHEDULE_ID = "scheduleId"
    const val LOG_ID = "logId"
    const val SENIOR_USER_ID = "seniorUserId"
    const val SENIOR_NAME = "seniorName"
}

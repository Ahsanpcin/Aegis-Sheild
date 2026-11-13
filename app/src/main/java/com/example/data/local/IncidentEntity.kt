package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.core.models.SecurityIncident
import com.example.core.models.ThreatCategory
import com.example.core.models.ThreatSeverity

@Entity(tableName = "security_incidents")
data class IncidentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestampMillis: Long,
    val packageName: String,
    val appDisplayName: String,
    val category: String,
    val severity: String,
    val confidenceScore: Float,
    val actionTaken: String,
    val diagnosticSummary: String = "",
    val lockdownDurationMinutes: Int = 0,
    val isReviewedFalsePositive: Boolean = false
) {
    fun toDomain(): SecurityIncident {
        val cat = try {
            ThreatCategory.valueOf(category)
        } catch (_: Exception) {
            ThreatCategory.EXPLICIT_ADULT
        }
        val sev = try {
            ThreatSeverity.valueOf(severity)
        } catch (_: Exception) {
            ThreatSeverity.HIGH
        }
        return SecurityIncident(
            id = id,
            timestampMillis = timestampMillis,
            packageName = packageName,
            appDisplayName = appDisplayName,
            category = cat,
            severity = sev,
            confidenceScore = confidenceScore,
            actionTaken = actionTaken,
            diagnosticSummary = diagnosticSummary,
            lockdownDurationMinutes = lockdownDurationMinutes,
            isReviewedFalsePositive = isReviewedFalsePositive
        )
    }

    companion object {
        fun fromDomain(incident: SecurityIncident): IncidentEntity {
            return IncidentEntity(
                id = incident.id,
                timestampMillis = incident.timestampMillis,
                packageName = incident.packageName,
                appDisplayName = incident.appDisplayName,
                category = incident.category.name,
                severity = incident.severity.name,
                confidenceScore = incident.confidenceScore,
                actionTaken = incident.actionTaken,
                diagnosticSummary = incident.diagnosticSummary,
                lockdownDurationMinutes = incident.lockdownDurationMinutes,
                isReviewedFalsePositive = incident.isReviewedFalsePositive
            )
        }
    }
}

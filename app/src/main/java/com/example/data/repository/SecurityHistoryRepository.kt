package com.example.data.repository

import com.example.core.models.SecurityIncident
import com.example.core.models.ThreatCategory
import com.example.data.local.IncidentDao
import com.example.data.local.IncidentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

data class DailyThreatCount(
    val dayLabel: String,
    val count: Int,
    val isToday: Boolean
)

data class ThreatAnalyticsSummary(
    val totalDetections: Int,
    val weeklyTrend: List<DailyThreatCount>,
    val explicitAdultCount: Int,
    val suggestiveCount: Int,
    val sensitiveGraphicCount: Int,
    val peakHourWindow: String,
    val peakAppDisplayName: String?
)

class SecurityHistoryRepository(private val dao: IncidentDao) {

    fun getIncidents(): Flow<List<SecurityIncident>> {
        return dao.getAllIncidents().map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getAnalytics(): Flow<ThreatAnalyticsSummary> {
        return getIncidents().map { computeAnalytics(it) }
    }

    fun getTotalDetectionsCount(): Flow<Int> = dao.getIncidentCount()

    fun getBlockedAppCount(): Flow<Int> = dao.getBlockedAppCount()

    fun getLatestIncident(): Flow<SecurityIncident?> {
        return dao.getLatestIncident().map { it?.toDomain() }
    }

    suspend fun logIncident(incident: SecurityIncident): Long {
        return dao.insertIncident(IncidentEntity.fromDomain(incident))
    }

    suspend fun markFalsePositive(incidentId: Long, isFalsePositive: Boolean) {
        dao.setFalsePositive(incidentId, isFalsePositive)
    }

    suspend fun deleteIncident(incidentId: Long) {
        dao.deleteIncident(incidentId)
    }

    suspend fun clearHistory() {
        dao.clearAll()
    }

    companion object {
        fun computeAnalytics(incidents: List<SecurityIncident>): ThreatAnalyticsSummary {
            val total = incidents.size
            val explicit = incidents.count { it.category == ThreatCategory.EXPLICIT_ADULT }
            val suggestive = incidents.count { it.category == ThreatCategory.SUGGESTIVE_CONTENT }
            val sensitive = incidents.count { it.category == ThreatCategory.SENSITIVE_GRAPHIC }

            val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            val weekly = mutableListOf<DailyThreatCount>()

            for (i in 6 downTo 0) {
                val targetCal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -i)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfDay = targetCal.timeInMillis
                val endOfDay = startOfDay + 86_400_000L - 1L
                val dayLabel = dayNames[targetCal.get(Calendar.DAY_OF_WEEK) - 1]
                val count = incidents.count { it.timestampMillis in startOfDay..endOfDay }
                weekly.add(DailyThreatCount(dayLabel = dayLabel, count = count, isToday = (i == 0)))
            }

            var nightCount = 0      // 00:00 - 05:59
            var morningCount = 0    // 06:00 - 11:59
            var afternoonCount = 0  // 12:00 - 17:59
            var eveningCount = 0    // 18:00 - 23:59

            val itemCal = Calendar.getInstance()
            for (incident in incidents) {
                itemCal.timeInMillis = incident.timestampMillis
                when (itemCal.get(Calendar.HOUR_OF_DAY)) {
                    in 0..5 -> nightCount++
                    in 6..11 -> morningCount++
                    in 12..17 -> afternoonCount++
                    else -> eveningCount++
                }
            }

            val peakWindow = when {
                total == 0 -> "No Threats Recorded"
                eveningCount >= morningCount && eveningCount >= afternoonCount && eveningCount >= nightCount -> "Evening (6 PM - 12 AM)"
                afternoonCount >= morningCount && afternoonCount >= nightCount -> "Afternoon (12 PM - 6 PM)"
                morningCount >= nightCount -> "Morning (6 AM - 12 PM)"
                else -> "Late Night (12 AM - 6 AM)"
            }

            val topApp = incidents
                .groupBy { it.appDisplayName.ifBlank { it.packageName } }
                .maxByOrNull { it.value.size }
                ?.key

            return ThreatAnalyticsSummary(
                totalDetections = total,
                weeklyTrend = weekly,
                explicitAdultCount = explicit,
                suggestiveCount = suggestive,
                sensitiveGraphicCount = sensitive,
                peakHourWindow = peakWindow,
                peakAppDisplayName = topApp
            )
        }
    }
}

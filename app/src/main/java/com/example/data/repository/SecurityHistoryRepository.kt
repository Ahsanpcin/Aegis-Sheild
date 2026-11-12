package com.example.data.repository

import com.example.core.models.SecurityIncident
import com.example.data.local.IncidentDao
import com.example.data.local.IncidentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SecurityHistoryRepository(private val dao: IncidentDao) {

    fun getIncidents(): Flow<List<SecurityIncident>> {
        return dao.getAllIncidents().map { list ->
            list.map { it.toDomain() }
        }
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
}

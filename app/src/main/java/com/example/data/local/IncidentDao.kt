package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {
    @Query("SELECT * FROM security_incidents ORDER BY timestampMillis DESC")
    fun getAllIncidents(): Flow<List<IncidentEntity>>

    @Query("SELECT COUNT(*) FROM security_incidents")
    fun getIncidentCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT packageName) FROM security_incidents")
    fun getBlockedAppCount(): Flow<Int>

    @Query("SELECT * FROM security_incidents ORDER BY timestampMillis DESC LIMIT 1")
    fun getLatestIncident(): Flow<IncidentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: IncidentEntity): Long

    @Query("UPDATE security_incidents SET isReviewedFalsePositive = :reviewed WHERE id = :incidentId")
    suspend fun setFalsePositive(incidentId: Long, reviewed: Boolean)

    @Query("DELETE FROM security_incidents WHERE id = :incidentId")
    suspend fun deleteIncident(incidentId: Long)

    @Query("DELETE FROM security_incidents")
    suspend fun clearAll()
}

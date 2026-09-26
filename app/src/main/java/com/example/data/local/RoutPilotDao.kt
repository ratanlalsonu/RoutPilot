package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutPilotDao {

    @Query("SELECT * FROM sensor_readings WHERE dataSource = :dataSource ORDER BY timestamp DESC LIMIT 50")
    fun observeRecentSensorReadings(dataSource: String): Flow<List<SensorReadingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSensorReading(reading: SensorReadingEntity)

    @Query("SELECT * FROM hazards WHERE status = 'ACTIVE' AND dataSource = :dataSource ORDER BY timestamp DESC")
    fun observeActiveHazardsBySource(dataSource: String): Flow<List<HazardEntity>>

    @Query("SELECT * FROM hazards ORDER BY timestamp DESC LIMIT 100")
    fun observeAllHazardsHistory(): Flow<List<HazardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHazard(hazard: HazardEntity)

    @Query("DELETE FROM hazards WHERE dataSource = :dataSource")
    suspend fun clearHazardsBySource(dataSource: String)

    @Query("SELECT * FROM road_status WHERE dataSource = :dataSource")
    fun observeRoadStatusesBySource(dataSource: String): Flow<List<RoadStatusEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoadStatus(roadStatus: RoadStatusEntity)

    @Query("DELETE FROM road_status WHERE dataSource = :dataSource")
    suspend fun clearRoadStatusesBySource(dataSource: String)

    @Query("SELECT * FROM route_requests ORDER BY timestamp DESC LIMIT 50")
    fun observeRouteRequests(): Flow<List<RouteRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRouteRequest(request: RouteRequestEntity)

    @Query("SELECT * FROM history_events ORDER BY timestamp DESC LIMIT 150")
    fun observeHistoryEvents(): Flow<List<HistoryEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryEvent(event: HistoryEventEntity)

    @Query("DELETE FROM history_events")
    suspend fun clearAllHistoryEvents()

    @Query("SELECT * FROM journeys ORDER BY startedAt DESC LIMIT 20")
    fun observeJourneys(): Flow<List<JourneyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJourney(journey: JourneyEntity)
}

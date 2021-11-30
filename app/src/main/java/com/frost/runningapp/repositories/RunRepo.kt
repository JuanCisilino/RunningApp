package com.frost.runningapp.repositories

import com.frost.runningapp.db.Run
import com.frost.runningapp.db.RunDAO
import javax.inject.Inject

class RunRepo @Inject constructor(val runDAO: RunDAO) {

    suspend fun insertRun(run: Run) = runDAO.insertRun(run)
    suspend fun deleteAllRuns() = runDAO.deleteAll()

    fun getAllRunsSortedByDate() = runDAO.getAllRunsSortedByDate()
    fun getAllRunsSortedByAverageSpeed() = runDAO.getAllRunsSortedByAverageSpeed()
    fun getAllRunsSortedByCaloriesBurned() = runDAO.getAllRunsSortedByCaloriesBurned()
    fun getAllRunsSortedByDistance() = runDAO.getAllRunsSortedByDistance()
    fun getAllRunsSortedByTimeInMillis() = runDAO.getAllRunsSortedByTimeInMillis()

    fun getTotalAverageSpeed() = runDAO.getTotalAverageSpeed()
    fun getTotalCaloriesBurned() = runDAO.getTotalCaloriesBurned()
    fun getTotalDistance() = runDAO.getTotalDistance()
    fun getTotalTimeInMillis() = runDAO.getTotalTimeInMillis()
}
package com.frost.runningapp.ui.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import com.frost.runningapp.repositories.RunRepo

class StadisticsViewModel @ViewModelInject constructor(
    val runRepo: RunRepo
): ViewModel() {

    val totalTimeRun = runRepo.getTotalTimeInMillis()
    val totalCaloriesBurned = runRepo.getTotalCaloriesBurned()
    val totalAverageSpeed = runRepo.getTotalAverageSpeed()
    val totalDistance = runRepo.getTotalDistance()

    val runsSortedByDate = runRepo.getAllRunsSortedByDate()

}
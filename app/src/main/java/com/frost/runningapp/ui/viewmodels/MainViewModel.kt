package com.frost.runningapp.ui.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frost.runningapp.db.Run
import com.frost.runningapp.helpers.SortType
import com.frost.runningapp.repositories.RunRepo
import kotlinx.coroutines.launch

class MainViewModel @ViewModelInject constructor(
    val runRepo: RunRepo
): ViewModel() {

    private val runSortedByDate = runRepo.getAllRunsSortedByDate()
    private val runsSortedByDistance = runRepo.getAllRunsSortedByDistance()
    private val runsSortedByCaloriesBurned = runRepo.getAllRunsSortedByCaloriesBurned()
    private val runsSortedByTimeInMillis = runRepo.getAllRunsSortedByTimeInMillis()
    private val runsSortedByAverageSpeed = runRepo.getAllRunsSortedByAverageSpeed()

    val runs = MediatorLiveData<List<Run>>()

    var sortType = SortType.DATE

    init {
        runs.addSource(runSortedByDate) { result ->
            if (sortType == SortType.DATE) {
                result?.let { runs.value = it }
            }
        }
        runs.addSource(runsSortedByDistance) { result ->
            if (sortType == SortType.DISTANCE) {
                result?.let { runs.value = it }
            }
        }
        runs.addSource(runsSortedByCaloriesBurned) { result ->
            if (sortType == SortType.CALORIES_BURNED) {
                result?.let { runs.value = it }
            }
        }
        runs.addSource(runsSortedByTimeInMillis) { result ->
            if (sortType == SortType.RUNNING_TIME) {
                result?.let { runs.value = it }
            }
        }
        runs.addSource(runsSortedByAverageSpeed) { result ->
            if (sortType == SortType.AVG_SPEED) {
                result?.let { runs.value = it }
            }
        }
    }

    fun deleteRuns() = viewModelScope.launch { runRepo.deleteAllRuns() }

    fun sortRuns(sortType: SortType) = when(sortType) {
        SortType.DATE -> runSortedByDate.value?.let { runs.value = it }
        SortType.AVG_SPEED -> runsSortedByAverageSpeed.value?.let { runs.value = it }
        SortType.RUNNING_TIME -> runsSortedByTimeInMillis.value?.let { runs.value = it }
        SortType.CALORIES_BURNED -> runsSortedByCaloriesBurned.value?.let { runs.value = it }
        SortType.DISTANCE -> runsSortedByDistance.value?.let { runs.value = it }
    }.also {
        this.sortType = sortType
    }

    fun insertRun(run: Run) = viewModelScope.launch { runRepo.insertRun(run) }
}
package com.frost.runningapp.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.frost.runningapp.R
import com.frost.runningapp.db.Run
import com.frost.runningapp.helpers.TrackingHelper
import com.frost.runningapp.ui.viewmodels.StadisticsViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_statistics.*
import kotlin.math.round

@AndroidEntryPoint
class StadisticsFragment: Fragment(R.layout.fragment_statistics) {

    private val viewModel: StadisticsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        barChart()
        subscribeToLiveData()
    }

    private fun barChart(){
        barChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawLabels(false)
            axisLineColor = Color.WHITE
            textColor = Color.WHITE
            setDrawGridLines(false)
        }
        barChart.axisLeft.apply {
            axisLineColor = Color.WHITE
            textColor = Color.WHITE
            setDrawGridLines(false)
        }
        barChart.axisRight.apply {
            axisLineColor = Color.WHITE
            textColor = Color.WHITE
            setDrawGridLines(false)
        }
        barChart.apply {
            description.text = "Avg Speed Over Time"
            legend.isEnabled = false
        }
    }

    private fun subscribeToLiveData() {
        viewModel.totalTimeRun.observe(viewLifecycleOwner, Observer { it?.let { setTotalTime(it) } })
        viewModel.totalDistance.observe(viewLifecycleOwner, Observer { it?.let { setTotalDistance(it) } })
        viewModel.totalAverageSpeed.observe(viewLifecycleOwner, Observer { it?.let{ setAvgSpeed(it) } })
        viewModel.totalCaloriesBurned.observe(viewLifecycleOwner, Observer { it?.let { setCalories(it) } })
        viewModel.runsSortedByDate.observe(viewLifecycleOwner, Observer { it?.let { setBarChart(it) } })
    }

    private fun setTotalTime(millis: Long) {
        tvTotalTime.text = TrackingHelper.getFormattedStopWatchTime(millis)
    }

    private fun setCalories(calories: Int) {
        val totalCalories = "${calories}kcal"
        tvTotalCalories.text = totalCalories
    }

    private fun setAvgSpeed(speed: Float) {
        val avgSpeed = round(speed * 10f) / 10f
        val avgSpeedString = "${avgSpeed}km/h"
        tvAverageSpeed.text = avgSpeedString
    }

    private fun setTotalDistance(distance: Int) {
        val km = distance / 1000f
        val totalDistance = round(km * 10f) / 10f
        val totalDistanceString = "${totalDistance}kms"
        tvTotalDistance.text = totalDistanceString
    }

    private fun setBarChart(list: List<Run>) {
        val allAvgSpeed = list.indices.map { i -> BarEntry(i.toFloat(), list[i].avgSpeedKMH) }
        val barDataSet = BarDataSet(allAvgSpeed, "Avg Speed Over Time").apply {
            valueTextColor = Color.WHITE
            color = ContextCompat.getColor(requireContext(), R.color.colorAccent)
        }
        barChart.data = BarData(barDataSet)
        barChart.invalidate()
    }

}
package com.frost.runningapp.ui.fragments

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.frost.runningapp.R
import com.frost.runningapp.ui.viewmodels.MainViewModel
import com.frost.runningapp.ui.viewmodels.StadisticsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StadisticsFragment: Fragment(R.layout.fragment_statistics) {

    private val viewModel: StadisticsViewModel by viewModels()

}
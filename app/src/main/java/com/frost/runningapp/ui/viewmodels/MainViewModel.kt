package com.frost.runningapp.ui.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import com.frost.runningapp.repositories.RunRepo

class MainViewModel @ViewModelInject constructor(
    val runRepo: RunRepo
): ViewModel() {
}
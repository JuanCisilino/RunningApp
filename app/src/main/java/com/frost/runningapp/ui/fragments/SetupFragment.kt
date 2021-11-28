package com.frost.runningapp.ui.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.frost.runningapp.R
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_setup.*
import javax.inject.Inject

@AndroidEntryPoint
class SetupFragment: Fragment(R.layout.fragment_setup) {

    @Inject
    lateinit var sharedPref : SharedPreferences

    @set:Inject
    var isFirstAppOpen = true


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkFirstOpen(savedInstanceState)
        setBtn()
    }

    private fun checkFirstOpen(savedInstanceState: Bundle?) {
        if (!isFirstAppOpen) {
            val navOptions = NavOptions.Builder().setPopUpTo(R.id.setupFragment, true).build()
            findNavController().navigate(R.id.action_setupFragment_to_runFragment, savedInstanceState,
                navOptions)
        }
    }

    private fun setBtn(){
        tvContinue.setOnClickListener {
            if (writePersonalDataFromSharedPref()) findNavController().navigate(R.id.action_setupFragment_to_runFragment)
            else Snackbar.make(requireView(), "Please enter all fields", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun writePersonalDataFromSharedPref(): Boolean {
        val name = etName.text.toString()
        val weight = etWeight.text.toString()
        if (name.isEmpty() or weight.isEmpty()) return false
        sharedPref.edit()
            .putString(getString(R.string.KEY_FIRST_TIME_NAME), name)
            .putFloat(getString(R.string.KEY_FIRST_TIME_WEIGHT), weight.toFloat())
            .putBoolean(getString(R.string.KEY_FIRST_TIME_TOGGLE), false)
            .apply()
        val toolbarText = "Let's go, $name!"
        requireActivity().tvToolbarTitle.text = toolbarText
        return true
    }
}
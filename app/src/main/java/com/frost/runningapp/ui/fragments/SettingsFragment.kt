package com.frost.runningapp.ui.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.frost.runningapp.R
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_settings.*
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment: Fragment(R.layout.fragment_settings) {

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadFieldsFromSharedPref()
        setBtn()
    }

    private fun setBtn(){
        btnApplyChanges.setOnClickListener {
            val success = applyChangesToSharedPref()
            if (success) Snackbar.make(it, "Saved changes", Snackbar.LENGTH_SHORT).show()
            else Snackbar.make(it, "Please fill out all the fields", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun loadFieldsFromSharedPref(){
        val name = sharedPreferences.getString(getString(R.string.KEY_FIRST_TIME_NAME), "")?: ""
        val weight = sharedPreferences.getFloat("KEY_FIRST_TIME_WEIGHT", 80f)
        etName.setText(name)
        etWeight.setText(weight.toString())
    }

    private fun applyChangesToSharedPref(): Boolean{
        val nameText = etName.text.toString()
        val weightText = etWeight.text.toString()
        if (nameText.isEmpty() or weightText.isEmpty()) return false
        sharedPreferences.edit()
            .putString(getString(R.string.KEY_FIRST_TIME_NAME), nameText)
            .putFloat(getString(R.string.KEY_FIRST_TIME_WEIGHT), weightText.toFloat())
            .apply()
        val toolbarText = "Let's go, $nameText"
        requireActivity().tvToolbarTitle.text = toolbarText
        return true
    }
}
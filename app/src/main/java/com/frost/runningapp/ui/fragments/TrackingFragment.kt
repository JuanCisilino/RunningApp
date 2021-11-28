package com.frost.runningapp.ui.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.frost.runningapp.R
import com.frost.runningapp.db.Run
import com.frost.runningapp.helpers.TrackingHelper
import com.frost.runningapp.services.Polyline
import com.frost.runningapp.services.TrackingService
import com.frost.runningapp.ui.viewmodels.MainViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_tracking.*
import java.util.*
import javax.inject.Inject
import kotlin.math.round

@AndroidEntryPoint
class TrackingFragment: Fragment(R.layout.fragment_tracking) {

    private val viewModel: MainViewModel by viewModels()

    private var map: GoogleMap?= null
    private var isTracking = false
    private var pathPoints = mutableListOf<Polyline>()
    private var currentTimeMillis = 0L
    private var menu: Menu? = null

    @set:Inject
    var weight = 80f

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
    : View? {
        setHasOptionsMenu(true)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView.onCreate(savedInstanceState)
        setBtn()
        setMapView()
        subscribeToObservers()
    }

    private fun setMapView(){
        mapView.getMapAsync {
            map = it
            addAllPolylines()
        }
    }

    private fun setBtn(){
        btnToggleRun.setOnClickListener { toggleRun() }
        btnFinishRun.setOnClickListener {
            zoomFullTrack()
            endRunAndSaveToDb()
        }
    }

    private fun subscribeToObservers() {
        TrackingService.isTracking.observe(viewLifecycleOwner, Observer { updateTracking(it) })
        TrackingService.pathPoints.observe(viewLifecycleOwner, Observer {
            pathPoints = it
            addLatestPolylines()
            moveCameraToUser()
        })
        TrackingService.timeRunInMillis.observe(viewLifecycleOwner, Observer {
            currentTimeMillis = it
            val formattedTime = TrackingHelper.getFormattedStopWatchTime(currentTimeMillis, true)
            tvTimer.text = formattedTime
        })
    }

    private fun toggleRun(){
        if (isTracking) {
            menu?.getItem(0)?.isVisible = true
            sendCommandToService(R.string.ACTION_PAUSE_SERVICE)
        } else {
            sendCommandToService(R.string.ACTION_START_OR_RESUME_SERVICE)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.toolbar_tracking_menu, menu)
        this.menu = menu
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        if (currentTimeMillis > 0L) {
            this.menu?.getItem(0)?.isVisible = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.calcelTracking -> showCancelTrackingDialog()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showCancelTrackingDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("Cancel Run")
            .setMessage("Are you sure???")
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton("yes") { _, _ -> stopRun()}
            .setNegativeButton("nop") { dialogInterface, _ -> dialogInterface.cancel() }
            .create()
        dialog.show()
    }

    private fun stopRun() {
        tvTimer.text = "00:00:00:00"
        sendCommandToService(R.string.ACTION_STOP_SERVICE)
        findNavController().navigate(R.id.action_trackingFragment_to_runFragment)
    }

    private fun updateTracking(isTracking: Boolean) {
        this.isTracking = isTracking
        if (!isTracking && currentTimeMillis > 0L) {
            btnToggleRun.text = "Start"
            btnFinishRun.visibility = View.VISIBLE
        } else if (isTracking){
            btnToggleRun.text = "Stop"
            menu?.getItem(0)?.isVisible = true
            btnFinishRun.visibility = View.GONE
        }
    }

    private fun moveCameraToUser(){
        if (pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()) {
            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    pathPoints.last().last(),
                    15f
                )
            )
        }
    }

    private fun zoomFullTrack(){
        val bounds = LatLngBounds.builder()
        pathPoints.forEach { polylines -> polylines.forEach { bounds.include(it) } }
        map?.moveCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds.build(),
                mapView.width,
                mapView.height,
                (mapView.height * 0.05f).toInt()
            )
        )
    }

    private fun endRunAndSaveToDb() {
        map?.snapshot { bmp ->
            var distanceInMts = 0
            pathPoints.forEach { polylines ->
                distanceInMts += TrackingHelper.calculatePolylineLength(polylines).toInt() }
            val avgSpeed = round((distanceInMts / 1000f) / (currentTimeMillis / 1000f / 60 / 60) * 10) / 10f
            val dateTimestamp = Calendar.getInstance().timeInMillis
            val caloriesBurned = ((distanceInMts / 1000f) * weight).toInt()
            val run = Run(bmp, dateTimestamp, avgSpeed, distanceInMts, currentTimeMillis, caloriesBurned)
            viewModel.insertRun(run)
            Snackbar.make(
                requireActivity().findViewById(R.id.rootView),
                "Run saved!!",
                Snackbar.LENGTH_LONG
            ).show()
            stopRun()
        }
    }

    private fun addAllPolylines(){
        pathPoints.forEach {
            val polylineOptions = PolylineOptions()
                .color(Color.RED)
                .width(8f)
                .addAll(it)
            map?.addPolyline(polylineOptions)
        }
    }

    private fun addLatestPolylines(){
        if (pathPoints.isNotEmpty() && pathPoints.last().size > 1) {
            val preLastLatLong = pathPoints.last()[pathPoints.last().size-2]
            val lastLatLong = pathPoints.last().last()
            val polylineOptions = PolylineOptions()
                .color(Color.RED)
                .width(8f)
                .add(preLastLatLong)
                .add(lastLatLong)
            map?.addPolyline(polylineOptions)
        }
    }

    private fun sendCommandToService(action: Int) =
        Intent(requireContext(), TrackingService::class.java).also {
            it.action = getString(action)
            requireContext().startService(it)
        }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }
}
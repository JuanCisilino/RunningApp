package com.frost.runningapp.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.frost.runningapp.R
import com.frost.runningapp.helpers.TrackingHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

typealias Polyline = MutableList<LatLng>
typealias Polylines = MutableList<Polyline>

@AndroidEntryPoint
class TrackingService: LifecycleService() {

    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    @Inject
    lateinit var baseNotificationBuilder : NotificationCompat.Builder

    lateinit var curNotificationBuilder: NotificationCompat.Builder

    private var isFirstRun = true
    private var serviceKilled = false
    private val timeRunInSeconds = MutableLiveData<Long>()

    companion object{
        val timeRunInMillis = MutableLiveData<Long>()
        val isTracking = MutableLiveData<Boolean>()
        val pathPoints = MutableLiveData<Polylines>()
    }

    private fun postInitialValues() {
        isTracking.postValue(false)
        pathPoints.postValue(mutableListOf())
        timeRunInSeconds.postValue(0L)
        timeRunInMillis.postValue(0L)
    }

    override fun onCreate() {
        super.onCreate()
        curNotificationBuilder = baseNotificationBuilder
        postInitialValues()
        fusedLocationProviderClient = FusedLocationProviderClient(this)

        isTracking.observe(this, Observer { update(it) })
    }

    private fun update(isTracking: Boolean) {
        updateLocationTracking(isTracking)
        updateNotificationTrackingState(isTracking)
    }

    private fun killService() {
        Timber.d("Stopped service")
        serviceKilled = true
        isFirstRun = true
        pauseService()
        postInitialValues()
        stopForeground(true)
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                getString(R.string.ACTION_START_OR_RESUME_SERVICE) ->
                    if (isFirstRun) startForegroundService() else startTime()
                getString(R.string.ACTION_PAUSE_SERVICE) -> pauseService()
                getString(R.string.ACTION_STOP_SERVICE) -> killService()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private var isTimerEnable = false
    private var lapTime = 0L
    private var runTime = 0L
    private var startedTime = 0L
    private var lastSecondTimestamp = 0L

    private fun startTime() {
        Timber.d("Resuming Service")
        addEmptyPolyline()
        setValuesTrue()
        CoroutineScope(Dispatchers.Main).launch {
            while (isTracking.value!!) {
                manageTracking()
                delay(50L)
            }
            runTime += lapTime
        }
    }

    private fun setValuesTrue(){
        isTracking.postValue(true)
        startedTime = System.currentTimeMillis()
        isTimerEnable = true
    }

    private fun manageTracking(){
        lapTime = System.currentTimeMillis() - startedTime
        timeRunInMillis.postValue(runTime + lapTime)
        if (timeRunInMillis.value!! >= lastSecondTimestamp + 1000L) {
            timeRunInSeconds.postValue(timeRunInSeconds.value!! + 1)
            lastSecondTimestamp += 1000L
        }
    }

    private fun pauseService(){
        Timber.d("Paused service")
        isTracking.postValue(false)
        isTimerEnable = false
    }

    private fun updateNotificationTrackingState(isTracking: Boolean){
        val notificationActionText = if (isTracking) "Pause" else "Resume"
        val pendingIntent = if (isTracking) {
            val pauseIntent = Intent(this, TrackingService::class.java).apply {
                action = getString(R.string.ACTION_PAUSE_SERVICE)
            }
            PendingIntent.getService(this, 1, pauseIntent, FLAG_UPDATE_CURRENT  )
        } else {
            val resumedIntent = Intent(this, TrackingService::class.java).apply {
                action = getString(R.string.ACTION_START_OR_RESUME_SERVICE)
            }
            PendingIntent.getService(this, 2, resumedIntent, FLAG_UPDATE_CURRENT  )
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        clearActions()
        if (!serviceKilled){
            curNotificationBuilder = baseNotificationBuilder
                .addAction(R.drawable.ic_pause_black_24dp, notificationActionText, pendingIntent)
            notificationManager.notify(1, curNotificationBuilder.build())
        }
    }

    private fun clearActions(){
        curNotificationBuilder.javaClass.getDeclaredField("mActions").apply {
            isAccessible = true
            set(curNotificationBuilder, ArrayList<NotificationCompat.Action>())
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateLocationTracking(isTracking: Boolean){
        if (isTracking){
            if (TrackingHelper.hasLocationPermission(this)){
                val request = LocationRequest().apply {
                    interval = 4000
                    fastestInterval = 1000
                    priority = PRIORITY_HIGH_ACCURACY
                }
                fusedLocationProviderClient.requestLocationUpdates(
                    request,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }
        } else {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }

    private val locationCallback = object : LocationCallback(){
        override fun onLocationResult(result: LocationResult?) {
            super.onLocationResult(result)
            if (isTracking.value!!) {
                result?.locations?.let { locations -> locations.forEach {
                    addPathPoint(it)
                    Timber.d("NEW LOCATION: ${it.latitude}, ${it.longitude}") }
                }
            }
        }
    }

    private fun addPathPoint(location: Location?) {
        location?.let {
            val pos = LatLng(it.latitude, it.longitude)
            pathPoints.value?.apply {
                last().add(pos)
                pathPoints.postValue(this)
            }
        }
    }

    private fun addEmptyPolyline() = pathPoints.value?.apply {
        add(mutableListOf())
        pathPoints.postValue(this)
    } ?: pathPoints.postValue(mutableListOf(mutableListOf()))

    private fun startForegroundService() {
        isFirstRun = false
        startTime()
        isTracking.postValue(true)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createNotificationChannel(notificationManager)
        startForeground(1, baseNotificationBuilder.build())
        timeRunInSeconds.observe(this, Observer {
            if (!serviceKilled){
                val notification = curNotificationBuilder
                    .setContentText(TrackingHelper.getFormattedStopWatchTime(it * 1000L))
                notificationManager.notify(1, notification.build())
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager){
        val channel = NotificationChannel(
            getString(R.string.NOTIFICATION_CHANNEL_ID),
            getString(R.string.NOTIFICATION_CHANNEL_NAME),
            IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }
}
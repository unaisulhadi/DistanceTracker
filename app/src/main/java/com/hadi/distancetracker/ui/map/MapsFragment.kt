package com.hadi.distancetracker.ui.map

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.hadi.distancetracker.R
import com.hadi.distancetracker.databinding.FragmentMapsBinding
import com.hadi.distancetracker.service.TrackerService
import com.hadi.distancetracker.ui.map.MapUtil.calculateDistance
import com.hadi.distancetracker.ui.map.MapUtil.calculateElapsedTime
import com.hadi.distancetracker.ui.map.MapUtil.setCameraPosition
import com.hadi.distancetracker.ui.result.Result
import com.hadi.distancetracker.util.Constants
import com.hadi.distancetracker.util.ExtensionFunctions.disable
import com.hadi.distancetracker.util.ExtensionFunctions.enable
import com.hadi.distancetracker.util.ExtensionFunctions.hide
import com.hadi.distancetracker.util.ExtensionFunctions.show
import com.hadi.distancetracker.util.Permissions
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MapsFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener,
    EasyPermissions.PermissionCallbacks, GoogleMap.OnMarkerClickListener {

    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding!!

    private lateinit var map: GoogleMap

    private var locationList = mutableListOf<LatLng>()
    private var polylineList = mutableListOf<Polyline>()

    private var markerList = mutableListOf<Marker>()

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    val started = MutableLiveData(false)


    var startTime = 0L
    var stopTime = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMapsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.tracking = this

        binding.buttonStart.setOnClickListener {
            onStartButtonClicked()
        }
        binding.buttonStop.setOnClickListener {
            onStopButtonClicked()
        }
        binding.buttonReset.setOnClickListener {
            onResetButtonClicked()
        }

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }


    private fun onStartButtonClicked() {
        if (Permissions.hasBackgroundLocationPermission(requireContext())) {
            startCountDown()
            binding.buttonStart.disable()
            binding.buttonStart.hide()
            binding.buttonStop.show()
        } else {
            Permissions.requestBackgroundLocationPermission(this)
        }
    }


    private fun onStopButtonClicked() {
        binding.buttonStop.hide()
        binding.buttonStart.show()
        stopForegroundService()
    }


    private fun onResetButtonClicked() {
        mapReset()
    }

    private fun stopForegroundService() {
        binding.buttonStart.disable()
        sendActionCommandToService(Constants.ACTION_SERVICE_STOP)
    }


    private fun startCountDown() {
        binding.timerTextView.show()
        binding.buttonStop.disable()
        val timer = object : CountDownTimer(4000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val currentSecond = millisUntilFinished / 1000
                if (currentSecond.toString() == "0") {
                    binding.timerTextView.text = "GO"
                    binding.timerTextView.setTextColor(ContextCompat.getColor(requireContext(),
                        R.color.black))
                } else {
                    binding.timerTextView.text = currentSecond.toString()
                    binding.timerTextView.setTextColor(ContextCompat.getColor(requireContext(),
                        R.color.red))
                }
            }

            override fun onFinish() {
                sendActionCommandToService(Constants.ACTION_SERVICE_START)
                binding.timerTextView.hide()
            }
        }
        timer.start()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms[0])) {
            SettingsDialog.Builder(requireActivity())
                .build()
                .show()
        } else {
            Permissions.requestBackgroundLocationPermission(this)
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        onStartButtonClicked()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.isMyLocationEnabled = true
        map.setOnMyLocationButtonClickListener(this)
        map.setOnMarkerClickListener(this)
        map.uiSettings.apply {
            isZoomControlsEnabled = false
            isZoomGesturesEnabled = false
            isRotateGesturesEnabled = false
            isCompassEnabled = false
            isTiltGesturesEnabled = false
            isScrollGesturesEnabled = false
            //isMyLocationButtonEnabled = true
        }
        observeTrackerService()
    }

    private fun observeTrackerService() {
        TrackerService.locationList.observe(viewLifecycleOwner) {
            if (it != null) {
                locationList = it
                if (locationList.isNotEmpty()) {
                    binding.buttonStop.enable()
                }
                drawPolyline()
                followPolyline()
            }
        }
        TrackerService.startTime.observe(viewLifecycleOwner) {
            startTime = it
        }
        TrackerService.stopTime.observe(viewLifecycleOwner) {
            stopTime = it
            if (stopTime != 0L) {
                showBiggerPicture()
                displayResults()
            }
        }
        TrackerService.started.observe(viewLifecycleOwner) {
            started.value = it
        }
    }


    private fun drawPolyline() {
        val polyLine = map.addPolyline(
            PolylineOptions().apply {
                width(10f)
                color(Color.BLUE)
                jointType(JointType.ROUND)
                startCap(ButtCap())
                endCap(ButtCap())
                addAll(locationList)
            }
        )
        polylineList.add(polyLine)
    }

    private fun followPolyline() {
        if (locationList.isNotEmpty()) {
            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    setCameraPosition(locationList.last())
                ), 1000, null)
        }
    }


    fun sendActionCommandToService(action: String) {
        Intent(requireContext(), TrackerService::class.java).apply {
            this.action = action
            requireContext().startService(this)
        }

    }

    override fun onMyLocationButtonClick(): Boolean {
        binding.hintTextView.animate()
            .alpha(0f)
            .duration = 1500
        lifecycleScope.launch {
            delay(2500)
            binding.hintTextView.hide()
            binding.buttonStart.show()
        }
        return false
    }

    private fun displayResults() {
        val result = com.hadi.distancetracker.ui.result.Result(
            calculateDistance(locationList),
            calculateElapsedTime(startTime, stopTime)
        )
        lifecycleScope.launch {
            delay(2500)
            val directions = MapsFragmentDirections.actionMapsFragmentToResultFragment(result)
            findNavController().navigate(directions)
            binding.buttonStart.apply {
                hide()
                enable()
            }
            binding.buttonStop.hide()
            binding.buttonReset.show()
        }
    }

    private fun showBiggerPicture() {

        val bounds = LatLngBounds.Builder()
        for (location in locationList) {
            bounds.include(location)
        }
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(
            bounds.build(),
            100,
        ), 2000, null)
        addMarker(locationList.first())
        addMarker(locationList.last())
    }

    private fun addMarker(position: LatLng) {
        val marker = map.addMarker(MarkerOptions().position(position))
        markerList.add(marker!!)
    }

    private fun mapReset() {
        fusedLocationProviderClient.lastLocation.addOnCompleteListener { task ->
            val lastKnownLocation = LatLng(
                task.result.latitude,
                task.result.longitude,
            )
            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    setCameraPosition(lastKnownLocation)
                )
            )
            for (polyline in polylineList) {
                polyline.remove()
            }
            for(marker in markerList){
                marker.remove()
            }
            locationList.clear()
            markerList.clear()
            binding.buttonReset.hide()
            binding.buttonStart.show()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        // Disables camera movement to marker when clicking marker
        return true
    }
}

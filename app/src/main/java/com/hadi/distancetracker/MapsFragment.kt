package com.hadi.distancetracker

import android.content.Intent
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.hadi.distancetracker.databinding.FragmentMapsBinding
import com.hadi.distancetracker.service.TrackerService
import com.hadi.distancetracker.util.Constants
import com.hadi.distancetracker.util.ExtensionFunctions.disable
import com.hadi.distancetracker.util.ExtensionFunctions.hide
import com.hadi.distancetracker.util.ExtensionFunctions.show
import com.hadi.distancetracker.util.Permissions
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MapsFragment : Fragment(), OnMapReadyCallback , GoogleMap.OnMyLocationButtonClickListener, EasyPermissions.PermissionCallbacks {

    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding!!

    private lateinit var map:GoogleMap

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMapsBinding.inflate(inflater, container, false)


        binding.buttonStart.setOnClickListener {
            onStartButtonClicked()
        }
        binding.buttonStop.setOnClickListener {  }
        binding.buttonReset.setOnClickListener {  }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }


    private fun onStartButtonClicked() {
        if(Permissions.hasBackgroundLocationPermission(requireContext())){
            startCountDown()
            binding.buttonStart.disable()
            binding.buttonStart.hide()
            binding.buttonStop.show()
        }else{
            Permissions.requestBackgroundLocationPermission(this)
        }
    }

    private fun startCountDown() {
        binding.timerTextView.show()
        binding.buttonStop.disable()
        val timer = object : CountDownTimer(4000L,1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val currentSecond = millisUntilFinished/1000
                if(currentSecond.toString() == "0"){
                    binding.timerTextView.text = "GO"
                    binding.timerTextView.setTextColor(ContextCompat.getColor(requireContext(),R.color.black))
                }else{
                    binding.timerTextView.text = currentSecond.toString()
                    binding.timerTextView.setTextColor(ContextCompat.getColor(requireContext(),R.color.red))
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
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,this)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if(EasyPermissions.somePermissionPermanentlyDenied(this,perms[0])) {
            SettingsDialog.Builder(requireActivity())
                .build()
                .show()
        }else{
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
        map.uiSettings.apply {
            isZoomControlsEnabled = false
            isZoomGesturesEnabled = false
            isRotateGesturesEnabled = false
            isCompassEnabled = false
            isTiltGesturesEnabled = false
            isScrollGesturesEnabled = false
            //isMyLocationButtonEnabled = true
        }
    }


    fun sendActionCommandToService(action : String) {
        Intent(requireContext(),TrackerService::class.java).apply {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
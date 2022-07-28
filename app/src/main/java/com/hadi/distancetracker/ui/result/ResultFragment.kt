package com.hadi.distancetracker.ui.result

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hadi.distancetracker.databinding.FragmentResultBinding


class ResultFragment : BottomSheetDialogFragment() {

    private val args : ResultFragmentArgs by navArgs()

    private var _binding: FragmentResultBinding ?= null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentResultBinding.inflate(inflater,container, false)

        binding.distanceValueTextView.text = args.result.distance +" km"
        binding.timeValueTextView.text = args.result.time


        binding.shareButton.setOnClickListener {
            shareResult()
        }

        return binding.root
    }

    private fun shareResult() {
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "I went ${args.result.distance} km in ${args.result.time}!")
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

    }

}
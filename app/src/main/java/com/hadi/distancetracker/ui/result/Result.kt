package com.hadi.distancetracker.ui.result

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Result(
    var distance: String,
    var time: String,
) : Parcelable


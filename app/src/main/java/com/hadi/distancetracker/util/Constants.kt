package com.hadi.distancetracker.util

object Constants {

    const val PERMISSION_LOCATION_REQUEST_CODE = 1
    const val PERMISSION_BG_LOCATION_REQUEST_CODE = 2

    const val ACTION_SERVICE_START = "ACTION_SERVICE_START"
    const val ACTION_SERVICE_STOP = "ACTION_SERVICE_STOP"
    const val ACTION_NAVIGATE_TO_MAP = "ACTION_NAVIGATE_TO_MAP"

    const val NOTIFICATION_CHANNEL_ID = "tracker_notification_id"
    const val NOTIFICATION_CHANNEL_NAME = "tracker_notification"
    const val NOTIFICATION_ID = 3

    const val PENDING_INTENT_REQ_CODE = 99

    const val LOCATION_UPDATE_INTERVAL = 4000L
    const val LOCATION_FASTEST_UPDATE_INTERVAL = 2000L
}
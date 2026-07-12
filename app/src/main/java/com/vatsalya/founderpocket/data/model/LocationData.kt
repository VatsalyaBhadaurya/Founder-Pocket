package com.vatsalya.founderpocket.data.model

data class LocationData(
    val lat: Double,
    val lng: Double,
    val label: String?       // reverse-geocoded place name, nullable
)

package io.github.domi04151309.podscompanion.data

data class StatusElement(
    val charge: Byte = 0,
    val charging: Boolean = false,
    val connected: Boolean = false
)
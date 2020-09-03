package io.github.domi04151309.podscompanion.data

data class StatusElement(
    var charge: Byte = 0,
    var charging: Boolean = false,
    var connected: Boolean = false
)
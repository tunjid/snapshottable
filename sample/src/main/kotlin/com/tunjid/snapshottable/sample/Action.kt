package com.tunjid.snapshottable.sample

sealed interface Action {
    data class ToggleSector(val sector: Sector) : Action
}

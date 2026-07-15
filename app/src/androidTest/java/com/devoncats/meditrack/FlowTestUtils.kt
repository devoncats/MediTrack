package com.devoncats.meditrack

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

// Room's Flow queries emit the table's current state immediately on collection (same as
// LiveData did on first observe), so first() with a timeout is the direct equivalent of the
// old observeForever-and-wait-for-onChanged helper this replaced.
suspend fun <T> Flow<T>.getOrAwaitValue(timeoutSeconds: Long = 5): T =
    withTimeout(timeoutSeconds * 1000) { first() }

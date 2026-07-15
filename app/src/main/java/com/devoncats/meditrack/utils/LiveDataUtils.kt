package com.devoncats.meditrack.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

/**
 * Combines two LiveData sources into one, re-emitting whenever either changes, once [sourceA]
 * has emitted at least once ([sourceB]'s absence is treated as [emptyB] rather than blocking).
 */
fun <A, B, R> combineLatest(
    sourceA: LiveData<A>,
    sourceB: LiveData<B>,
    emptyB: B,
    combine: (A, B) -> R
): LiveData<R> = MediatorLiveData<R>().apply {
    fun update() {
        val a = sourceA.value ?: return
        value = combine(a, sourceB.value ?: emptyB)
    }
    addSource(sourceA) { update() }
    addSource(sourceB) { update() }
}

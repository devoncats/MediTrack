package com.devoncats.meditrack

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

fun <T> LiveData<T>.getOrAwaitValue(timeoutSeconds: Long = 5): T? {
    var result: T? = null
    val latch = CountDownLatch(1)
    val observer = object : Observer<T> {
        override fun onChanged(value: T) {
            result = value
            latch.countDown()
            removeObserver(this)
        }
    }
    InstrumentationRegistry.getInstrumentation().runOnMainSync { observeForever(observer) }
    latch.await(timeoutSeconds, TimeUnit.SECONDS)
    return result
}

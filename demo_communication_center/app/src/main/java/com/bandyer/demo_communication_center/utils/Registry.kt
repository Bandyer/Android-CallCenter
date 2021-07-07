package com.bandyer.demo_communication_center.utils

import kotlinx.coroutines.flow.StateFlow

interface Registry<T> {
    val records: StateFlow<HashMap<String, T>>
    fun add(id: String, state: T)
    fun update(id: String, state: T)
    fun remove(id: String)
}
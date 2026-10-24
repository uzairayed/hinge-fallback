package com.hingefallback

import java.util.concurrent.CopyOnWriteArrayList

object StatusStore {
    @Volatile var hall0: String = "Waiting"
    @Volatile var hall1: String = "Waiting"
    @Volatile var deviceState: String = "Unknown"
    @Volatile var lastError: String = ""

    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    fun listen(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun unlisten(listener: () -> Unit) {
        listeners.remove(listener)
    }

    fun publish() {
        listeners.forEach { it.invoke() }
    }
}

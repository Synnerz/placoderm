package com.github.synnerz.placoderm.internal

import com.github.synnerz.placoderm.event.Event
import com.github.synnerz.placoderm.event.EventBus
import com.github.synnerz.placoderm.event.EventListener

/**
 * - Abstract Api class with [on] helper method
 */
abstract class Api : IApi {
    /**
     * - registers a new event listener to the specified event
     */
    inline fun <reified T : Event> on(noinline cb: (T) -> Unit): EventListener<T> {
        val listener = EventBus.on<T>(cb, false)
        return listener
    }
}
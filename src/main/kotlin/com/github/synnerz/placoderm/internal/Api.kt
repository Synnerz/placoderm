package com.github.synnerz.placoderm.internal

import com.github.synnerz.placoderm.Placoderm
import com.github.synnerz.placoderm.event.Event
import com.github.synnerz.placoderm.event.EventBus
import com.github.synnerz.placoderm.event.EventListener

interface Api {
    val minecraft get() = Placoderm.minecraft

    fun onInitialize() {}
}

inline fun <reified T : Event> Api.on(noinline cb: (T) -> Unit): EventListener<T> {
    val listener = EventBus.on<T>(cb, false)
    return listener
}
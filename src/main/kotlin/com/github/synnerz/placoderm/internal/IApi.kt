package com.github.synnerz.placoderm.internal

import com.github.synnerz.placoderm.Placoderm
import com.github.synnerz.placoderm.event.Event
import com.github.synnerz.placoderm.event.EventBus
import com.github.synnerz.placoderm.event.EventListener

interface IApi {
    val minecraft get() = Placoderm.minecraft

    fun onInitialize() {}
}
package com.github.synnerz.placoderm.event

import com.github.synnerz.placoderm.state.Register
import kotlin.reflect.KClass

class EventListener<T : Event> (
    private val cb: (T) -> Unit,
    private val event: KClass<T>,
) : Register() {
    var prio: Int = 0
        set(value) {
            field = value
            if (isRegistered()) {
                unregister()
                register()
            }
        }

    @Suppress("UNCHECKED_CAST")
    override fun add() {
        EventBus.add(event, this as EventListener<Event>)
    }

    @Suppress("UNCHECKED_CAST")
    override fun remove() {
        EventBus.remove(event, this as EventListener<Event>)
    }

    fun trigger(event: T) {
        cb(event)
    }
}
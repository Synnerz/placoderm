package com.github.synnerz.placoderm.state

import kotlinx.atomicfu.atomic
import java.util.concurrent.CopyOnWriteArrayList

open class BasicState<T>(initial: T) : State<T> {
    private val v = atomic(initial)
    private val listeners = CopyOnWriteArrayList<(v: T) -> Unit>()

    override var value: T
        get() = v.value
        set(value) {
            if (v.getAndSet(value) != value) listeners.forEach { it(value) }
        }

    override fun listen(cb: (v: T) -> Unit) {
        listeners.add(cb)
    }

    override fun <R> map(transform: (v: T) -> R) = UnaryDerivedState(this, transform)

    override fun <O, R> zip(other: State<O>, transform: (a: T, b: O) -> R) =
        BinaryDerivedState(this, other, transform)
}
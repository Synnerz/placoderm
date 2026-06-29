package com.github.synnerz.placoderm.state

interface State<T> {
    var value: T
    fun listen(cb: (v: T) -> Unit)
    fun <R> map(transform: (v: T) -> R): State<R>
    fun <O, R> zip(other: State<O>, transform: (a: T, b: O) -> R): State<R>
    fun debug(name: String) = apply {
        listen {
            println("$name changed to $it")
        }
    }
}

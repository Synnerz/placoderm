package com.github.synnerz.placoderm.state

class BinaryDerivedState<T1, T2, R>(
    private val base1: State<T1>,
    private val base2: State<T2>,
    private val transform: (a: T1, b: T2) -> R
) : BasicState<R>(transform(base1.value, base2.value)) {
    override var value: R
        get() = super.value
        set(value) {
            throw UnsupportedOperationException()
        }

    init {
        base1.listen {
            super.value = transform(it, base2.value)
        }
        base2.listen {
            super.value = transform(base1.value, it)
        }
    }
}
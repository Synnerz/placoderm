package com.github.synnerz.placoderm.state

class UnaryDerivedState<T, R>(
    base: State<T>,
    private val transform: (v: T) -> R
) : BasicState<R>(transform(base.value)) {
    override var value: R
        get() = super.value
        set(value) {
            throw UnsupportedOperationException()
        }

    init {
        base.listen {
            super.value = transform(it)
        }
    }
}
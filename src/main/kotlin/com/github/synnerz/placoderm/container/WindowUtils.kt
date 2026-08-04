package com.github.synnerz.placoderm.container

import com.github.synnerz.placoderm.internal.Api

object WindowUtils : Api() {
    val window get() = minecraft.window
    val width get() = window.width
    val height get() = window.height
    val guiScale get() = window.guiScale
    val scaledWidth get() = window.guiScaledWidth
    val scaledHeight get() = window.guiScaledHeight

    fun fromScale(scale: Int): Pair<Int, Int>
        = width(scale) to height(scale)

    fun width(scale: Int) = width / scale

    fun height(scale: Int) = height / scale
}
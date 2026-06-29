package com.github.synnerz.placoderm.container

import com.github.synnerz.placoderm.internal.Api
import kotlin.math.max

object MouseUtils : Api {
    val mouseHandler by lazy { minecraft.mouseHandler }
    val x get() = x(WindowUtils.guiScale)
    val y get() = y(WindowUtils.guiScale)

    fun x(scale: Int): Double
        = mouseHandler.xpos() * scale / max(1, WindowUtils.width)

    fun y(scale: Int): Double
        = mouseHandler.ypos() * scale / max(1, WindowUtils.height)
}
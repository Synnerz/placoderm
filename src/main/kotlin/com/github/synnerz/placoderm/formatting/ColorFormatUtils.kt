package com.github.synnerz.placoderm.formatting

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import java.util.Optional

object ColorFormatUtils {
    val colorCodesRegex = "[\\u00a7&][0-9a-fk-or]".toRegex(RegexOption.IGNORE_CASE)
    val colorToFormat = ChatFormatting.entries.mapNotNull { format ->
        TextColor.fromLegacyFormat(format)?.let { it to format }
    }.toMap()
    val escapedAmp = "&{2}".toRegex()
    val ampToSection = "&(?=[0-9a-fklmnor])".toRegex()

    fun String.clearCodes(): String = this.replace(colorCodesRegex, "")

    fun String.replaceCodes(): String = this
        .split(escapedAmp)
        .joinToString("&") {
            it.replace(ampToSection, "§")
        }

    fun fromStyle(style: Style): String = buildString {
        append("§r")

        style.color?.let(colorToFormat::get)?.run(::append)

        when {
            style.isBold -> append("§l")
            style.isItalic -> append("§o")
            style.isUnderlined -> append("§n")
            style.isStrikethrough -> append("§m")
            style.isObfuscated -> append("§k")
        }
    }

    private fun parseFormat(_text: Component): String {
        var str = ""

        _text.contents.visit({ style, text ->
            val styleFormat = fromStyle(style)
            str += "${styleFormat}$text"
            Optional.empty<Any>()
        }, _text.style)

        return str
    }

    fun fromLegacy(string: String): Component {
        val component = Component.literal("")
        var oldStr = ""
        var hadSS = false
        var style = Style.EMPTY

        for (char in string) {
            if (char == '§') {
                hadSS = true
                if (oldStr.isNotEmpty()) {
                    component.append(Component.literal(oldStr).withStyle(style))
                    oldStr = ""
                }
                continue
            }
            if (hadSS && (char.isLetter() || char.isDigit())) {
                hadSS = false

                style = when (char) {
                    'l' -> style.withBold(true)
                    'o' -> style.withItalic(true)
                    'n' -> style.withUnderlined(true)
                    'm' -> style.withStrikethrough(true)
                    'k' -> style.withObfuscated(true)
                    '0' -> Style.EMPTY.withColor(TextColor.BLACK)
                    '1' -> Style.EMPTY.withColor(TextColor.DARK_BLUE)
                    '2' -> Style.EMPTY.withColor(TextColor.DARK_GREEN)
                    '3' -> Style.EMPTY.withColor(TextColor.DARK_AQUA)
                    '4' -> Style.EMPTY.withColor(TextColor.DARK_RED)
                    '5' -> Style.EMPTY.withColor(TextColor.DARK_PURPLE)
                    '6' -> Style.EMPTY.withColor(TextColor.GOLD)
                    '7' -> Style.EMPTY.withColor(TextColor.GRAY)
                    '8' -> Style.EMPTY.withColor(TextColor.DARK_GRAY)
                    '9' -> Style.EMPTY.withColor(TextColor.BLUE)
                    'a' -> Style.EMPTY.withColor(TextColor.GREEN)
                    'b' -> Style.EMPTY.withColor(TextColor.AQUA)
                    'c' -> Style.EMPTY.withColor(TextColor.RED)
                    'd' -> Style.EMPTY.withColor(TextColor.LIGHT_PURPLE)
                    'e' -> Style.EMPTY.withColor(TextColor.YELLOW)
                    'f' -> Style.EMPTY.withColor(TextColor.WHITE)
                    'r' -> Style.EMPTY
                    else -> Style.EMPTY
                }
                continue
            }

            oldStr += char
        }
        if (oldStr.isNotEmpty())
            component.append(Component.literal(oldStr).withStyle(style))

        return component
    }

    fun String.fromLegacy(): Component = fromLegacy(this)

    fun Component.colorCodes(): String {
        var str = parseFormat(this)

        str += this.siblings.joinToString("", transform = ::parseFormat)

        return str
    }

    fun colorForNumber(num: Double, max: Double) = when {
        num >= max * 0.75 -> "§2"
        num >= max * 0.50 -> "§e"
        num >= max * 0.25 -> "§6"
        else -> "§4"
    }

    fun colorForNumber(num: Int, max: Int) = colorForNumber(num.toDouble(), max.toDouble())
    fun colorForNumber(num: Float, max: Float) = colorForNumber(num.toDouble(), max.toDouble())
    fun colorForNumber(num: Long, max: Long) = colorForNumber(num.toDouble(), max.toDouble())
}
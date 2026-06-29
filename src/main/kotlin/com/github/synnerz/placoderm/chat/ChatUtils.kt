package com.github.synnerz.placoderm.chat

import com.github.synnerz.placoderm.event.TickEvent
import com.github.synnerz.placoderm.internal.Api
import com.github.synnerz.placoderm.internal.on
import com.github.synnerz.placoderm.mixin.accessor.ChatComponentAccessor
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.chat.GuiMessage
import net.minecraft.network.chat.Component
import java.util.IdentityHashMap

object ChatUtils : Api {
    private var needRefresh = 0
    val chatLineIds = mutableMapOf<GuiMessage, Int>()
    val lineCache = IdentityHashMap<GuiMessage.Line, GuiMessage>()
    val chatGui get() = Minecraft.getInstance().gui.hud.chat
    val chatGuiAccessor get() = chatGui as ChatComponentAccessor

    data class TextComponent(var text: Component, var id: Int = 0)

    override fun onInitialize() {
        on<TickEvent> {
            if (needRefresh > 1) chatGuiAccessor.invokeRefresh()
            needRefresh = 0
        }
    }

    fun refreshChat() {
        needRefresh++
        if (needRefresh == 1) chatGuiAccessor.invokeRefresh()
    }
}
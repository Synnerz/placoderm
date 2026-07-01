package com.github.synnerz.placoderm.chat

import com.github.synnerz.placoderm.event.TickEvent
import com.github.synnerz.placoderm.internal.Api
import com.github.synnerz.placoderm.internal.on
import com.github.synnerz.placoderm.mixin.accessor.ChatComponentAccessor
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.impl.command.client.ClientCommandInternals
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.client.multiplayer.chat.GuiMessage
import net.minecraft.client.multiplayer.chat.GuiMessageSource
import net.minecraft.client.multiplayer.chat.GuiMessageTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import java.util.IdentityHashMap
import kotlin.math.roundToInt

object ChatUtils : Api {
    private var needRefresh = 0
    val chatLineIds = mutableMapOf<GuiMessage, Int>()
    val lineCache = IdentityHashMap<GuiMessage.Line, GuiMessage>()
    val chatGui get() = minecraft.gui.hud.chat
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

    fun literal(string: String): MutableComponent {
        return Component.literal(string.replace("&", "§"))
    }

    @JvmOverloads
    fun forTextComponent(text: Component, id: Int = 0): TextComponent {
        return TextComponent(text, id)
    }

    fun sendMessageWithId(message: Component, id: Int) {
        if (!minecraft.isMultiplayerServer)
            chatGui.addClientSystemMessage(message)
        else
            chatGui.addServerSystemMessage(message)

        chatLineIds[chatGuiAccessor.messages[0]] = id
    }

    fun sendMessage(message: Component) {
        minecraft.execute {
            minecraft.player?.sendSystemMessage(message)
        }
    }

    fun sendMessage(message: String) = sendMessage(literal(message))

    @JvmOverloads
    fun command(command: String, clientSide: Boolean = false) {
        if (!clientSide) return minecraft.connection!!.sendCommand(command)
        ClientCommandInternals.executeCommand(
            command,
            minecraft.connection!!.suggestionsProvider as FabricClientCommandSource,
            null
        )
    }

    fun say(message: String) {
        val connection = minecraft.connection ?: return
        if (message.startsWith("/")) return connection.sendCommand(message.drop(1))

        connection.sendChat(message)
    }

    fun sendActionbar(message: Component) {
        minecraft.execute {
            minecraft.player?.sendOverlayMessage(message)
        }
    }

    fun sendActionbar(message: String) = sendActionbar(literal(message))

    fun getMessageFromLine(line: GuiMessage.Line): GuiMessage? = lineCache[line]

    fun centerTextPadding(text: String): String {
        val textRenderer = minecraft.font
        val ww = minecraft.options.chatWidth()
        val chatWidth = ChatComponent.getWidth(ww.get())
        val textWidth = textRenderer.width(text)
        if (textWidth >= chatWidth) return text

        val padding = (chatWidth - textWidth) / 2f
        val paddingBuilder = StringBuilder().apply {
            repeat((padding / textRenderer.width(" ")).roundToInt()) {
                append(' ')
            }
        }

        return paddingBuilder.toString()
    }

    fun removeLines(cb: (GuiMessage) -> Boolean) {
        var removedLine = false
        val messageList = chatGuiAccessor.messages?.listIterator() ?: return

        while (messageList.hasNext()) {
            val msg = messageList.next()
            if (!cb(msg)) continue

            messageList.remove()
            chatLineIds.remove(msg)
            removedLine = true
        }

        if (!removedLine) return

        refreshChat()
    }

    fun editLines(cb: (GuiMessage) -> Boolean, replaceWith: TextComponent) {
        var editedLine = false
        val indicator =
            if (!minecraft.isMultiplayerServer) GuiMessageTag.systemSinglePlayer()
            else GuiMessageTag.system()
        val messageList = chatGuiAccessor.messages?.listIterator() ?: return

        while (messageList.hasNext()) {
            val msg = messageList.next()
            if (!cb(msg)) continue

            editedLine = true
            messageList.remove()
            chatLineIds.remove(msg)

            val line = GuiMessage(msg.addedTime, replaceWith.text, null, GuiMessageSource.SYSTEM_SERVER, indicator)
            chatLineIds[line] = replaceWith.id
            messageList.add(line)
        }

        if (!editedLine) return

        refreshChat()
    }

    @JvmOverloads
    fun deleteMessage(comp: Component, max: Int = 20, cb: ((Component, Component) -> Boolean)? = null) {
        val iter = chatGuiAccessor.messages.listIterator()
        var i = max

        while (--i >= 0 && iter.hasNext()) {
            val line = iter.next()
            val cbRes = cb?.invoke(line.content, comp) ?: false

            if (line.content === comp || cbRes) {
                iter.remove()
                refreshChat()
                break
            }
        }
    }
}
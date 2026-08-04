package com.github.synnerz.placoderm.event

import com.github.synnerz.placoderm.scheduler.Scheduler
import com.github.synnerz.placoderm.statistics.Ping
import com.github.synnerz.placoderm.formatting.ColorFormatUtils.clearCodes
import com.github.synnerz.placoderm.internal.IApi
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.common.ClientboundPingPacket
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.network.protocol.game.ClientboundTabListPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.entity.EntityType
import net.minecraft.world.phys.Vec3
import org.lwjgl.glfw.GLFW
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.KClass
import kotlin.reflect.full.hasAnnotation

object EventBus : IApi {
    var totalTicks = 0
    var clientTicks = 0
    private val teamRegex = "^team_(\\d+)$".toRegex()
    val events = ConcurrentHashMap<KClass<*>, MutableList<EventListener<Event>>>()
    private val entityTypes = mutableMapOf<Int, EntityType<*>>()
    private val entityPos = mutableMapOf<Int, Vec3>()
    var _internalSkipPing = Collections.newSetFromMap<Int>(ConcurrentHashMap())

    override fun onInitialize() {
        ClientEntityEvents.ENTITY_LOAD.register { entity, _ ->
            EntityJoinEvent(entity).post()
        }
        ClientEntityEvents.ENTITY_UNLOAD.register { entity, _ ->
            EntityLeaveEvent(entity).post()
        }
        ClientTickEvents.START_CLIENT_TICK.register {
            TickEvent(it, clientTicks++).post()
        }
//        LevelRenderEvents.START_MAIN.register {
//            (it.levelRenderer() as? LevelRendererAccessor)?.let { Render3DState.bufferSource = it.renderBuffers.stagedVertexBuffer() }
//        }
        ClientLifecycleEvents.CLIENT_STARTED.register {
            GameLoadEvent(it).post()
        }
        ClientLifecycleEvents.CLIENT_STOPPING.register {
            GameUnloadEvent(it).post()
        }
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register { mc, world ->
            WorldChangeEvent(mc, world).post()
            totalTicks = 0
            entityTypes.clear()
            entityPos.clear()
            _internalSkipPing.clear()
        }
        ScreenEvents.BEFORE_INIT.register { _, screen, _, _ ->
            ScreenMouseEvents.allowMouseClick(screen).register { _, event ->
                !GuiClickEvent(
                    event.x,
                    event.y,
                    event.button(),
                    true,
                    screen,
                    event
                ).post()
            }

            ScreenMouseEvents.allowMouseRelease(screen).register { _, event ->
                !GuiClickEvent(
                    event.x,
                    event.y,
                    event.button(),
                    false,
                    screen,
                    event
                ).post()
            }

            ScreenKeyboardEvents.allowKeyPress(screen).register { _, event ->
                !GuiKeyDownEvent(
                    GLFW.glfwGetKeyName(event.key, event.scancode),
                    event.key,
                    event.scancode,
                    screen,
                    event,
                ).post()
            }

            ScreenKeyboardEvents.allowKeyRelease(screen).register { _, event ->
                !GuiKeyUpEvent(
                    GLFW.glfwGetKeyName(event.key, event.scancode),
                    event.key,
                    event.scancode,
                    screen,
                    event,
                ).post()
            }
        }
        LevelRenderEvents.AFTER_BLOCK_OUTLINE_EXTRACTION.register { worldContext, hitResult ->
            val cancel = BeforeBlockOutlineEvent(worldContext, hitResult).post()
            if (cancel) worldContext.levelState().blockOutlineRenderState = null
            !cancel
        }
        ClientReceiveMessageEvents.ALLOW_GAME.register { comp, overlay ->
            val str = comp.string.clearCodes()

            if (overlay) return@register !ActionbarEvent(str, comp).post()

            val specialized = ChatChannelEvent.from(str, comp)
            val b1 = ChatEvent(str, comp).post()
            val b2 = specialized?.post() ?: false

            return@register !b1 && !b2
        }
        ClientReceiveMessageEvents.MODIFY_GAME.register { comp, overlay ->
            val str = comp.string.clearCodes()

            val evn = if (overlay) ModifyActionbarEvent(str, comp)
            else ModifyChatEvent(str, comp)

            evn.post()

            return@register evn.overrideValue
        }

        on<PacketReceivedEvent> { event ->
            when (val packet = event.packet) {
                is ClientboundSoundPacket -> {
                    val sound = packet.sound.unwrapKey().getOrNull()?.identifier() ?: return@on
                    if (SoundPlayEvent(
                            "${sound.namespace}:${sound.path}",
                            packet.pitch,
                            packet.volume,
                            packet.source,
                            packet.x, packet.y, packet.z,
                            packet.seed,
                            packet.sound.value()
                        ).post()) event.cancel()
                }

                is ClientboundPlayerInfoUpdatePacket -> {
                    val action = packet.actions().firstOrNull() ?: return@on
                    if (action === ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER) {
                        packet.entries().forEach {
                            val name = it.displayName ?: return@forEach
                            TabAddEvent(name.string.clearCodes(), name).post()
                        }
                        return@on
                    }
                    if (action !== ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME) return@on

                    packet.entries().forEach {
                        val name = it.displayName ?: return@forEach
                        TabUpdateEvent(name.string.clearCodes(), name).post()
                    }
                    return@on
                }

                is ClientboundTabListPacket -> {
                    packet.footer.string.split("\n").forEach { TabFooterEvent(it, packet.footer).post() }
                    packet.header.string.split("\n").forEach { TabHeaderEvent(it, packet.header).post() }
                }

                is ClientboundPingPacket -> {
                    if (packet.id >= 0) return@on
                    totalTicks++
                    ServerTickEvent(totalTicks).post()
                }

                is ClientboundSetPlayerTeamPacket -> {
                    if (packet.parameters.isEmpty) return@on
                    val team = packet.parameters.get()
                    val teamPrefix = team.playerPrefix.string
                    val teamSuffix = team.playerSuffix.string
                    if (teamPrefix.isEmpty()) return@on
                    if (!packet.name.matches(teamRegex)) return@on
                    ScoreboardEvent("${teamPrefix}${teamSuffix.trim()}".clearCodes(), packet).post()
                    return@on
                }

                is ClientboundAddEntityPacket -> {
                    val id = packet.id
                    val type = packet.type
                    entityTypes[id] = type
                    entityPos[id] = Vec3(packet.x, packet.y, packet.z)
                }

                is ClientboundSetEntityDataPacket -> {
                    val id = packet.id
                    val type = entityTypes[id] ?: return@on
                    val data = packet.packedItems
                    EntityDataEvent(id, type, data).post()
                    val text = getNameFromData(data)
                    if (text != null) NameChangeEvent(id, type, text, text.string).post()
                }

                is ClientboundSetActionBarTextPacket -> {
                    val text = packet.text
                    val message = text.string.clearCodes()

                    ActionbarEvent(message, text).post()
                }

                is ClientboundSetEquipmentPacket -> {
                    val id = packet.entity
                    val type = entityTypes[id] ?: return@on
                    val pos = entityPos[id] ?: return@on
                    EntityEquipmentEvent(
                        id,
                        type,
                        pos,
                        packet.slots.map { Pair(it.first, it.second) },
                    ).post()
                }

                is ClientboundSectionBlocksUpdatePacket -> {
                    MultiBlockUpdateEvent(packet).post()
                }

                is ClientboundBlockUpdatePacket -> {
                    BlockUpdateEvent(packet.pos, packet.blockState).post()
                }

                is ClientboundOpenScreenPacket -> {
                    ServerContainerOpenEvent(packet.containerId, packet.title, packet.title.string).post()
                }

                is ClientboundContainerClosePacket -> {
                    ServerContainerCloseEvent(packet.containerId).post()
                }

                is ClientboundContainerSetContentPacket -> {
                    ServerContainerSetContentEvent(
                        packet.containerId,
                        packet.stateId,
                        packet.items,
                        packet.carriedItem
                    ).post()
                }

                is ClientboundContainerSetSlotPacket -> {
                    if (ServerContainerSetSlotEvent(
                            packet.containerId,
                            packet.stateId,
                            packet.item,
                            packet.slot
                        ).post()
                    ) event.cancel()
                }
            }
        }

        on<PrePacketSentEvent> { event ->
            when (val packet = event.packet) {
                is ServerboundUseItemOnPacket -> {
                    UseItemOnEvent(packet.hitResult, packet.hand).post()
                }

                is ServerboundUseItemPacket -> {
                    UseItemEvent(packet.hand).post()
                }

                is ServerboundContainerClosePacket -> {
                    if (ClientContainerCloseEvent(packet.containerId).post())
                        event.cancel()
                }
            }
        }

        ClientLifecycleEvents.CLIENT_STARTED.register { client ->
            PostClientInitEvent(client).post()
        }
    }

    @JvmOverloads
    fun serverTicks(usingPing: Boolean = false): Int =
        if (usingPing) totalTicks + (Ping.getMedianPing() / 50.0 + 10.0).toInt()
        else totalTicks

    private fun getNameFromData(list: List<SynchedEntityData.DataValue<*>>): Component? {
        val idx = when (list.size) {
            8 -> 7
            9 -> 8
            16 -> 10
            17 -> 10
            19 -> 11
            22 -> 11
            else -> -1
        }

        val entry: SynchedEntityData.DataValue<*>?
        if (idx >= 0 && list[idx].id == 2) entry = list[idx]
        else entry = list.find { it.id == 2 }

        val v = entry?.value ?: return null
        return (v as? Optional<*>)?.getOrNull() as? Component
    }

    inline fun <reified T : Event> on(noinline cb: (T) -> Unit): EventListener<T> {
        return on<T>(cb, true)
    }

    inline fun <reified T : Event> on(noinline cb: (T) -> Unit, add: Boolean = true): EventListener<T> {
        val obj = EventListener(cb, T::class)
        if (add) obj.register()
        return obj
    }

    inline fun <reified T : Event> once(noinline cb: (T) -> Unit) {
        var evn: EventListener<T>? = null
        evn = on {
            evn!!.unregister()
            cb(it)
        }
    }

    private fun removeImpl(T: KClass<*>, listener: EventListener<*>) {
        events[T]?.remove(listener)
    }

    fun remove(T: KClass<*>, listener: EventListener<*>) {
        if (T.hasAnnotation<Threaded>()) removeImpl(T, listener)
        else Scheduler.scheduleTask { removeImpl(T, listener) }
    }

    private fun addImpl(T: KClass<*>, listener: EventListener<Event>) {
        val arr = events.getOrPut(T) {
            if (T.hasAnnotation<Threaded>()) CopyOnWriteArrayList()
            else ArrayList()
        }
        if (T.hasAnnotation<Ordered>()) {
            var i = arr.binarySearch(listener, Comparator.comparingInt { it.prio })
            if (i < 0) i = -(i + 1)
            arr.add(i, listener)
        } else arr.add(listener)
    }

    fun add(T: KClass<*>, listener: EventListener<Event>) {
        if (T.hasAnnotation<Threaded>()) addImpl(T, listener)
        else Scheduler.scheduleTask { addImpl(T, listener) }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Event> post(event: T) {
        val listeners = events[event::class] ?: return
        if (listeners.isEmpty()) return

        listeners.forEach { cb ->
            cb.trigger(event)
        }
    }
}
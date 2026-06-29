package com.github.synnerz.placoderm.scheduler

import com.github.synnerz.placoderm.event.ClientThreadServerTickEvent
import com.github.synnerz.placoderm.internal.Api
import com.github.synnerz.placoderm.event.GameUnloadEvent
import com.github.synnerz.placoderm.internal.on
import kotlinx.atomicfu.atomic
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.ScheduledExecutorService

object Scheduler : Api {
    private val taskComp = compareBy<Task>({ it.delay }, { it.id })
    private val tasks = PriorityBlockingQueue<Task>(10, taskComp)
    private var tick = atomic(0)
    private val tasksServer = PriorityBlockingQueue<Task>(10, taskComp)
    private var tickServer = atomic(0)
    private var taskId = atomic(0)
    private val beforePacketTasks = ConcurrentLinkedQueue<() -> Unit>()
    private val afterPacketTasks = ConcurrentLinkedQueue<() -> Unit>()

    val schedulePool: ScheduledExecutorService = Executors.newScheduledThreadPool(0) { r ->
        Thread(r, "Placoderm-Scheduler")
    }

    data class Task(var delay: Int, val cb: () -> Unit, val id: Int)

    override fun onInitialize() {
        ClientTickEvents.START_CLIENT_TICK.register {
            val curr = tick.incrementAndGet()
            while (tasks.isNotEmpty() && tasks.peek().delay <= curr) {
                val task = tasks.poll() ?: return@register
                task.cb()
            }
        }
        on<ClientThreadServerTickEvent> {
            val curr = tickServer.incrementAndGet()
            while (tasksServer.isNotEmpty() && tasksServer.peek().delay <= curr) {
                val task = tasksServer.poll() ?: return@on
                task.cb()
            }
        }
        on<GameUnloadEvent> {
            schedulePool.shutdown()
        }
    }

    @JvmOverloads
    fun scheduleTask(delay: Int = 1, cb: () -> Unit) {
        tasks.add(Task(tick.value + delay, cb, taskId.incrementAndGet()))
    }

    @JvmOverloads
    fun scheduleServerTask(delay: Int = 1, cb: () -> Unit) {
        tasksServer.add(Task(tickServer.value + delay, cb, taskId.incrementAndGet()))
    }

    fun scheduleBeforePacket(cb: () -> Unit) {
        beforePacketTasks.offer(cb)
    }

    fun scheduleAfterPacket(cb: () -> Unit) {
        afterPacketTasks.offer(cb)
    }

    fun internalListenerBefore() {
        var l = beforePacketTasks.size
        while (--l >= 0) {
            val cb = beforePacketTasks.poll() ?: break
            cb()
        }
    }

    fun internalListenerAfter() {
        var l = afterPacketTasks.size
        while (--l >= 0) {
            val cb = afterPacketTasks.poll() ?: break
            cb()
        }
    }
}
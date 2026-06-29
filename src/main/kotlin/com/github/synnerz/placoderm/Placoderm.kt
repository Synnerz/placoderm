package com.github.synnerz.placoderm

import com.github.synnerz.placoderm.chat.ChatUtils
import com.github.synnerz.placoderm.event.EventBus
import com.github.synnerz.placoderm.location.Location
import com.github.synnerz.placoderm.mod.PlacoInitializer
import com.github.synnerz.placoderm.scheduler.Scheduler
import com.github.synnerz.placoderm.statistics.Ping

object Placoderm : PlacoInitializer("placoderm") {
	var start = 0L
	override fun onPreApiInitialize() {
		log("loading api")
		start = System.currentTimeMillis()
		apis.addAll(listOf(
			EventBus,
			ChatUtils,
			Scheduler,
			Location,
			Ping,
		))
	}

	override fun onPostApiInitialize() {
		log("post api load ${System.currentTimeMillis() - start}ms")
	}
}

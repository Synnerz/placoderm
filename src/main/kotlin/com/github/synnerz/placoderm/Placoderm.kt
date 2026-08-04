package com.github.synnerz.placoderm

import com.github.synnerz.placoderm.chat.ChatUtils
import com.github.synnerz.placoderm.event.EventBus
import com.github.synnerz.placoderm.http.WebRequests
import com.github.synnerz.placoderm.location.Location
import com.github.synnerz.placoderm.mayor.Mayor
import com.github.synnerz.placoderm.mod.PlacoInitializer
import com.github.synnerz.placoderm.scheduler.Scheduler
import com.github.synnerz.placoderm.statistics.Ping
import com.google.gson.GsonBuilder

object Placoderm : PlacoInitializer("placoderm") {
	val gson = GsonBuilder().setPrettyPrinting().create()!!

	override fun onPreApiInitialize() {
		apis.addAll(listOf(
			EventBus,
			ChatUtils,
			Scheduler,
			Location,
			Ping,
			WebRequests.DEFAULT,
			Mayor,
		))
	}
}

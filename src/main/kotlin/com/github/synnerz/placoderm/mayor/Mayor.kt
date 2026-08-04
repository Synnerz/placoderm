package com.github.synnerz.placoderm.mayor

import com.github.synnerz.placoderm.Placoderm
import com.github.synnerz.placoderm.event.Event
import com.github.synnerz.placoderm.http.WebRequests
import com.github.synnerz.placoderm.internal.Api
import com.github.synnerz.placoderm.scheduler.Scheduler
import java.util.concurrent.TimeUnit

object Mayor : Api() {
    var mayorData: MayorData? = null

    data class MayorPerkData(
        val name: String,
        val description: String,
    )
    data class MinisterPerkData(
        val name: String,
        val description: String,
        val minister: Boolean,
    )
    data class MinisterData(
        val key: String, // category
        val name: String,
        val perk: MinisterPerkData?
    )
    data class MayorData(
        val key: String, // category
        val name: String,
        val perks: List<MayorPerkData>?,
        val minister: MinisterData?,
        val election: Any, // doesn't matter currently
    )
    data class MayorResponse(
        val success: Boolean,
        val lastUpdated: Long,
        val mayor: MayorData,
        val current: Any, // doesn't matter currently
    )

    class MayorUpdated(val data: Mayor) : Event

    override fun onInitialize() {
        Scheduler.schedulePool.scheduleWithFixedDelay(::update, 1L, 21L, TimeUnit.MINUTES)
    }

    fun update() {
        WebRequests.DEFAULT.withName("MayorAPI") {
            val response = WebRequests.DEFAULT.get("https://api.hypixel.net/v2/resources/skyblock/election")
            if (response.isEmpty()) return@withName

            val data = Placoderm.gson.fromJson(response, MayorResponse::class.java)
            val mayor = data.mayor
            mayorData = mayor
            MayorUpdated(this@Mayor).post()
            println("Placoderm\$MayorAPI(name=${mayor.name}, perks=${mayor.perks?.joinToString { "${it.name}, " }}, minister=${mayor.minister?.name}, ministerPerk=${mayor.minister?.perk?.name})")
        }
    }

    fun hasEzPz(): Boolean {
        if (mayorData == null) return false
        return mayorData!!.perks?.any { it.name.lowercase() == "ezpz" } ?: false || mayorData!!.minister?.perk?.name?.lowercase() == "ezpz"
    }

    fun hasRitual(): Boolean {
        if (mayorData == null) return false
        return mayorData!!.perks?.any { it.name.lowercase() == "mythological ritual" } ?: false || mayorData!!.minister?.perk?.name?.lowercase() == "mythological ritual"
    }

    fun isDerpy(): Boolean =
        if (mayorData == null)
            false
        else
            mayorData!!.name.lowercase() == "derpy" && mayorData!!.key == "derp"
}
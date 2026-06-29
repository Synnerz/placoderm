package com.github.synnerz.placoderm.mod

import com.github.synnerz.placoderm.internal.Api
import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

abstract class PlacoInitializer(val modId: String) : ClientModInitializer {
    private val logger = LoggerFactory.getLogger(modId)
    val apis = mutableListOf<Api>()
    val minecraft by lazy { Minecraft.getInstance() }

    // TODO: perhaps there are better ways to do this and also naming
    override fun onInitializeClient() {
        onPreInitialize()
        onPreApiInitialize()
        apis.forEach(Api::onInitialize)
        onPostApiInitialize()
        onPreLoad()
        onPreCommand()
        onPostCommand()
        onPostLoad()
        onPostInitialize()
    }

    // TODO: document
    // runs after onPreInitialize is called
    open fun onPreApiInitialize() {}
    // runs after apis have been initialized (and after onPreApiInitialize)
    open fun onPostApiInitialize() {}

    // runs before api initializes
    open fun onPreInitialize() {}
    // runs after everything is done initializing
    open fun onPostInitialize() {}

    // runs after api is initialized
    open fun onPreLoad() {}
    // runs after onPostCommand is called
    open fun onPostLoad() {}

    // runs after preLoad is called
    open fun onPreCommand() {}
    // runs after preCommand is called
    open fun onPostCommand() {}

    fun log(message: String)
        = logger.info(message)

    fun identifier(path: String): Identifier
        = Identifier.fromNamespaceAndPath(modId, path)
}
package com.github.synnerz.placoderm.container

import com.github.synnerz.placoderm.internal.Api
import com.github.synnerz.placoderm.mixin.accessor.AbstractContainerScreenAccessor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

object ScreenUtils : Api() {
    @JvmOverloads
    fun click(slot: Int, shift: Boolean = false, button: String = "LEFT"): Boolean {
        val screen = minecraft.gui.screen() ?: return false
        val container = screen as? AbstractContainerScreen<*> ?: return false
        if (slot > container.menu.items.size) return false
        val windowId = container.menu.containerId
        val clickMode = when {
            button == "MIDDLE" -> ContainerInput.CLONE
            shift -> ContainerInput.QUICK_MOVE
            else -> ContainerInput.PICKUP
        }
        val clickButton = when (button) {
            "LEFT" -> 0
            "RIGHT" -> 1
            "MIDDLE" -> 2
            else -> 0
        }
        val player = minecraft.player ?: return false
        minecraft.gameMode?.handleContainerInput(
            windowId,
            slot, clickButton, clickMode, player
        )

        return true
    }

    @JvmOverloads
    fun cursorStack(screen: Screen? = null): ItemStack? = cursorSlot(screen)?.item

    @JvmOverloads
    fun cursorSlot(screen: Screen? = null): Slot?
        = slotAt(MouseUtils.x, MouseUtils.y, screen)

    @JvmOverloads
    fun slotAt(x: Double, y: Double, screen: Screen? = null): Slot? {
        val nscreen = screen ?: minecraft.gui.screen() ?: return null
        if (!(nscreen is InventoryScreen || nscreen is ContainerScreen)) return null
        val container = nscreen as? AbstractContainerScreenAccessor ?: return null

        return container.getSlotAt(x, y)
    }
}
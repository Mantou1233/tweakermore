package me.fallenbreath.tweakermore.mixins.tweaks.tweakmTradyLapis;

import net.minecraft.client.gui.screen.ingame.ContainerScreen;
import net.minecraft.container.Slot;
import net.minecraft.container.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ContainerScreen.class)
public interface ContainerScreenAccessor
{
	@Invoker
	void invokeOnMouseClick(Slot slot, int invSlot, int button, SlotActionType slotActionType);
}

package me.fallenbreath.tweakermore.mixins.tweaks.tweakmTrady;

import me.fallenbreath.tweakermore.impl.tweakmTrady.TradyMerchantContainer;
import net.minecraft.client.gui.screen.ingame.ContainerScreen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.container.MerchantContainer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends ContainerScreen<MerchantContainer>
{
	public MerchantScreenMixin(MerchantContainer container, PlayerInventory playerInventory, Text name)
	{
		super(container, playerInventory, name);
	}

	@Inject(method = "init", at = @At("HEAD"))
	private void onInitScreen(CallbackInfo ci)
	{
		((TradyMerchantContainer)this.container).setMerchantScreen((MerchantScreen)(Object)this);
	}
}

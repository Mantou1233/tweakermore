package me.fallenbreath.tweakermore.mixins.tweaks.tweakmTradyLapis;

import me.fallenbreath.tweakermore.impl.tweakmTrady.MerchantContainerInScreen;
import me.fallenbreath.tweakermore.impl.tweakmTrady.TradingHelper;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.container.MerchantContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantContainer.class)
public abstract class MerchantContainerMixin implements MerchantContainerInScreen
{
	@Unique
	private MerchantScreen merchantScreen;

	@Inject(method = "setOffers", at = @At("TAIL"))
	private void onInitScreen(CallbackInfo ci)
	{
		new TradingHelper(merchantScreen).process();
	}

	@Override
	public void setMerchantScreen(MerchantScreen merchantScreen)
	{
		this.merchantScreen = merchantScreen;
	}

	@Override
	public MerchantScreen getMerchantScreen()
	{
		return this.merchantScreen;
	}
}

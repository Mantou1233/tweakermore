package me.fallenbreath.tweakermore.mixins.tweaks.tweakmTradyLapis;

import me.fallenbreath.tweakermore.impl.tweakmTrady.TradingHelper;
import me.fallenbreath.tweakermore.impl.tweakmTrady.TradyMerchantContainer;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.container.Container;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.SetTradeOffersS2CPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin
{
	@Nullable
	@Unique
	private TradingHelper tradingHelper;

	@Inject(
			method = "onSetTradeOffers",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/container/MerchantContainer;setRefreshTrades(Z)V",
					shift = At.Shift.AFTER
			),
			locals = LocalCapture.CAPTURE_FAILHARD
	)
	private void onInitTradeOffer(SetTradeOffersS2CPacket packet, CallbackInfo ci, Container container)
	{
		this.tradingHelper = new TradingHelper(((TradyMerchantContainer) container).getMerchantScreen());
		this.tradingHelper.process();
	}

	@Inject(
			method = "onInventory",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/container/Container;updateSlotStacks(Ljava/util/List;)V",
					shift = At.Shift.AFTER
			)
	)
	private void timeForTrady(InventoryS2CPacket packet, CallbackInfo ci)
	{
		if (this.tradingHelper != null && this.tradingHelper.getContainerId() == packet.getGuiId())
		{
			this.tradingHelper.doTrade();
		}
		this.tradingHelper = null;
	}
}

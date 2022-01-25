package me.fallenbreath.tweakermore.mixins.tweaks.tweakmTradyLapis;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import me.fallenbreath.tweakermore.impl.tweakmTrady.AbstractTradingHelper;
import me.fallenbreath.tweakermore.impl.tweakmTrady.FarmerTradingHelper;
import me.fallenbreath.tweakermore.impl.tweakmTrady.LapisTradingHelper;
import me.fallenbreath.tweakermore.impl.tweakmTrady.TradyMerchantContainer;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.container.Container;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.SetTradeOffersS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin
{
	@Unique
	private final List<AbstractTradingHelper> tradingHelpers = Lists.newArrayList();

	@Unique
	private static final List<Function<MerchantScreen, AbstractTradingHelper>> tradingHelperConstructors = ImmutableList.of(
			LapisTradingHelper::new, FarmerTradingHelper::new
	);

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
		MerchantScreen merchantScreen = ((TradyMerchantContainer) container).getMerchantScreen();
		this.tradingHelpers.clear();
		tradingHelperConstructors.forEach(ctr -> {
			AbstractTradingHelper tradingHelper = ctr.apply(merchantScreen);
			if (tradingHelper.isEnabled())
			{
				tradingHelper.checkOffer();
				this.tradingHelpers.add(tradingHelper);
			}
		});
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
		if (!this.tradingHelpers.isEmpty())
		{
			List<AbstractTradingHelper> toRemove = this.tradingHelpers.stream().
					filter(helper -> helper.getContainerId() != packet.getGuiId() || !helper.hasPendingTrade()).
					collect(Collectors.toList());
			this.tradingHelpers.removeAll(toRemove);
			this.tradingHelpers.forEach(AbstractTradingHelper::doTrade);
		}
	}
}

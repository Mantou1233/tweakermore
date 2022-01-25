package me.fallenbreath.tweakermore.impl.tweakmTrady;

import fi.dy.masa.malilib.util.InfoUtils;
import me.fallenbreath.tweakermore.config.TweakerMoreToggles;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TraderOfferList;

public class LapisTradingHelper extends AbstractTradingHelper
{
	private static final ItemStack LAPIS_LAZULI_2x = new ItemStack(Items.LAPIS_LAZULI, 2);

	public LapisTradingHelper(MerchantScreen merchantScreen)
	{
		super(merchantScreen);
	}

	@Override
	public boolean isEnabled()
	{
		return TweakerMoreToggles.TWEAKM_TRADY_LAPIS.getBooleanValue() && this.testProfession("entity.minecraft.villager.cleric");
	}

	@Override
	protected boolean shouldCloseContainerAfterTrade()
	{
		return true;
	}

	@Override
	public void checkOffer()
	{
		TraderOfferList traderOfferList = this.container.getRecipes();
		int rottenFreshIdx = -1;
		int lapisLazuliIdx = -1;
		int redstoneIdx = -1;
		boolean hasLapis = false;
		boolean hasNiceLapis = false;
		String reason = null;
		for (int i = 0; i < traderOfferList.size(); i++)
		{
			TradeOffer tradeOffer = traderOfferList.get(i);
			hasLapis |= offerMatches(tradeOffer, EMERALD_1x, EMPTY, Items.LAPIS_LAZULI);
			boolean thisOfferIsNiceLapis = offerMatches(tradeOffer, EMERALD_1x, EMPTY, LAPIS_LAZULI_2x);
			hasNiceLapis |= thisOfferIsNiceLapis;
			if (tradeOffer.isDisabled())
			{
				if (thisOfferIsNiceLapis)
				{
					reason = "locked";
				}
				continue;
			}
			if (thisOfferIsNiceLapis)
			{
				lapisLazuliIdx = i;
			}
			if (offerMatches(tradeOffer, EMERALD_1x, EMPTY, Items.REDSTONE) && tradeOffer.getUses() == 0)
			{
				redstoneIdx = i;
			}
			if (offerMatches(tradeOffer, Items.ROTTEN_FLESH, EMPTY, EMERALD_1x) && tradeOffer.getUses() == 0)
			{
				rottenFreshIdx = i;
			}
		}
		if (hasLapis && !hasNiceLapis)
		{
			reason = "profiteer";
		}
		if (lapisLazuliIdx != -1)
		{
			this.prepareTrade(lapisLazuliIdx, true);
		}
		else if (!hasLapis && rottenFreshIdx != -1)
		{
			this.prepareTrade(rottenFreshIdx, false);
		}
		else if (hasNiceLapis && redstoneIdx != -1)
		{
			this.prepareTrade(redstoneIdx, false);
		}
		else
		{
			InfoUtils.printActionbarMessage("[Trady Lapis] %1$s is useless now" + (reason != null ? ", reason: " + reason : ""), this.merchantScreen.getTitle());
			closeContainer();
		}
	}
}

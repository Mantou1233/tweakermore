package me.fallenbreath.tweakermore.impl.tweakmTrady;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.util.InfoUtils;
import me.fallenbreath.tweakermore.config.TweakerMoreToggles;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TraderOfferList;

import java.util.List;

public class FarmerTradingHelper extends AbstractTradingHelper
{
	private static final List<Item> TARGET_ITEMS = ImmutableList.of(Items.CARROT, Items.POTATO, Items.PUMPKIN, Items.MELON);

	public FarmerTradingHelper(MerchantScreen merchantScreen)
	{
		super(merchantScreen);
	}

	@Override
	public boolean isEnabled()
	{
		return TweakerMoreToggles.TWEAKM_TRADY_FARMER.getBooleanValue() && this.testProfession("entity.minecraft.villager.farmer");
	}

	@Override
	public void checkOffer()
	{
		TraderOfferList traderOfferList = this.container.getRecipes();
		for (int i = 0; i < traderOfferList.size(); i++)
		{
			TradeOffer tradeOffer = traderOfferList.get(i);
			if (tradeOffer.isDisabled())
			{
				continue;
			}
			for (Item item : TARGET_ITEMS)
			{
				if (offerMatches(tradeOffer, item, EMPTY, EMERALD))
				{
					if (this.canTrade(tradeOffer, false))
					{
						this.prepareTrade(i, true);
						return;
					}
					break;
				}
			}
		}
		InfoUtils.printActionbarMessage("Cannot trade with %1$s", this.merchantScreen.getTitle());
		closeContainer();
	}
}

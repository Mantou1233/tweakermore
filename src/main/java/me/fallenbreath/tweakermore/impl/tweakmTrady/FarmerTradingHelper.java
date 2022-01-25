package me.fallenbreath.tweakermore.impl.tweakmTrady;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import fi.dy.masa.malilib.util.InfoUtils;
import me.fallenbreath.tweakermore.config.TweakerMoreToggles;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TraderOfferList;

import java.util.List;

public class FarmerTradingHelper extends AbstractTradingHelper
{
	private static final List<Item> TARGET_ITEMS = ImmutableList.of(Items.CARROT, Items.POTATO, Items.PUMPKIN, Items.MELON);
	private Item itemThisTrade;
	private List<Item> tradedGoods = Lists.newArrayList();

	public FarmerTradingHelper(MerchantScreen merchantScreen)
	{
		super(merchantScreen);
	}

	@Override
	public boolean isEnabled()
	{
		return TweakerMoreToggles.TWEAKM_TRADY_FARMER.getBooleanValue() && (this.testProfession("entity.minecraft.villager.farmer") || this.container.getRecipes().size() >= 1 && this.container.getRecipes().get(0).getAdjustedFirstBuyItem().getItem() == Items.WHEAT);
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
		if (this.tradedGoods.isEmpty())
		{
			InfoUtils.printActionbarMessage("[Trady Farmer] Cannot trade with %1$s", this.merchantScreen.getTitle());
		}
		else
		{
			Text goods = this.tradedGoods.stream().map(Item::getName).reduce((sum, v) -> sum.append(", ").append(v)).orElseThrow(RuntimeException::new);
			InfoUtils.printActionbarMessage("Traded %1$s from %2$s", new LiteralText("[").append(goods).append("]"), this.merchantScreen.getTitle());
		}
		closeContainer();
	}

	@Override
	protected boolean shouldCloseContainerAfterTrade()
	{
		return false;
	}

	@Override
	protected void prepareTrade(int offerIndex, boolean tradeAll)
	{
		super.prepareTrade(offerIndex, tradeAll);
		this.itemThisTrade = this.container.getRecipes().get(offerIndex).getAdjustedFirstBuyItem().getItem();
	}

	@Override
	public int doTrade()
	{
		int amount = super.doTrade();
		if (amount > 0)
		{
			this.tradedGoods.add(this.itemThisTrade);
		}
		this.checkOffer();  // the container will close if no more goods
		return amount;
	}
}

package me.fallenbreath.tweakermore.impl.tweakmTrady;

import com.google.common.collect.Lists;
import fi.dy.masa.malilib.util.InfoUtils;
import me.fallenbreath.tweakermore.config.TweakerMoreConfigs;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TraderOfferList;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FarmerTradingHelper extends AbstractTradingHelper
{
	private Item itemThisTrade;
	private final List<Item> tradedGoods = Lists.newArrayList();

	public FarmerTradingHelper(MerchantScreen merchantScreen)
	{
		super(merchantScreen);
	}

	@Override
	public boolean isEnabled()
	{
		return TweakerMoreConfigs.TWEAKM_TRADY_FARMER.getBooleanValue() && (this.testProfession("entity.minecraft.villager.farmer") || this.container.getRecipes().size() >= 1 && this.container.getRecipes().get(0).getAdjustedFirstBuyItem().getItem() == Items.WHEAT);
	}

	private boolean findOffer(Predicate<TradeOffer> offerPredicate)
	{
		TraderOfferList traderOfferList = this.container.getRecipes();
		for (int i = 0; i < traderOfferList.size(); i++)
		{
			TradeOffer tradeOffer = traderOfferList.get(i);
			if (!tradeOffer.isDisabled() && offerPredicate.test(tradeOffer) && this.canTrade(tradeOffer, false))
			{
				this.prepareTrade(i, true);
				return true;
			}
		}
		return false;
	}

	@Override
	public void checkOffer()
	{
		List<Item> targetItems = TweakerMoreConfigs.TRADY_FARMER_TARGETS.getStrings().stream().
			map(itemId -> Registry.ITEM.getOrEmpty(new Identifier(itemId)).orElse(null)).
			filter(Objects::nonNull).
			collect(Collectors.toList());
		if (this.findOffer(offer -> targetItems.stream().anyMatch(item -> offerMatches(offer, item, EMPTY, EMERALD))))
		{
			return;
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
		TradeOffer tradeOffer = this.container.getRecipes().get(offerIndex);
		this.itemThisTrade = tradeOffer.getAdjustedFirstBuyItem().getItem();
		if (this.itemThisTrade == Items.EMERALD)  // buying
		{
			this.itemThisTrade = tradeOffer.getSellItem().getItem();
		}
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

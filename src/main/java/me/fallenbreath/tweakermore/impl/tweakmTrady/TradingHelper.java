package me.fallenbreath.tweakermore.impl.tweakmTrady;

import fi.dy.masa.malilib.util.InfoUtils;
import me.fallenbreath.tweakermore.config.TweakerMoreToggles;
import me.fallenbreath.tweakermore.mixins.tweaks.tweakmTradyLapis.ContainerScreenAccessor;
import me.fallenbreath.tweakermore.mixins.tweaks.tweakmTradyLapis.MerchantScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.container.MerchantContainer;
import net.minecraft.container.SlotActionType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.SelectVillagerTradeC2SPacket;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TraderOfferList;

import java.util.Objects;
import java.util.function.BiConsumer;

public class TradingHelper
{
	private final MerchantScreen merchantScreen;
	private final MerchantContainer container;

	public TradingHelper(MerchantScreen merchantScreen)
	{
		this.merchantScreen = merchantScreen;
		this.container = merchantScreen.getContainer();
	}

	public void process()
	{
		if (TweakerMoreToggles.TWEAKM_TRADY_LAPIS.getBooleanValue())
		{
			MinecraftClient mc = MinecraftClient.getInstance();
			TraderOfferList traderOfferList = this.container.getRecipes();
			for (int i = 0; i < traderOfferList.size(); i++)
			{
				TradeOffer tradeOffer = traderOfferList.get(i);
				if (
						ItemStack.areItemsEqual(tradeOffer.getOriginalFirstBuyItem(), new ItemStack(Items.EMERALD, 1)) &&
						tradeOffer.getSecondBuyItem().isEmpty() &&
						ItemStack.areItemsEqual(tradeOffer.getSellItem(), new ItemStack(Items.LAPIS_LAZULI, 2)) &&
						!tradeOffer.isDisabled()
				)
				{
					this.trade(i, true);
					break;
				}
			}
			mc.execute(() -> {
			});
		}
	}

	private void trade(int offerIndex, boolean tradeAll)
	{
		// select slot
		MinecraftClient mc = MinecraftClient.getInstance();
		((MerchantScreenAccessor)this.merchantScreen).setSelectedIndex(offerIndex);
		Objects.requireNonNull(mc.getNetworkHandler());
		Objects.requireNonNull(mc.player);
		mc.getNetworkHandler().sendPacket(new SelectVillagerTradeC2SPacket(offerIndex));

		// trade
		TradeOffer offer = this.container.getRecipes().get(offerIndex);
		int counter = 0;
		while (canTrade(offer) && counter++ < 100)
		{
			this.transact(offer);

			if (!tradeAll)
			{
				break;
			}
		}
	}

	private void iteratePlayerInventory(BiConsumer<ItemStack, Integer> consumer)
	{
		for (int i = 3; i < this.container.slots.size(); i++)
		{
			consumer.accept(this.container.slots.get(i).getStack(), i);
		}
	}

	private boolean canTrade(TradeOffer offer)
	{
		if (offer.isDisabled())
		{
			InfoUtils.printActionbarMessage("Offer disabled");
			return false;
		}
		if (!this.inputSlotsAreEmpty() || !Objects.requireNonNull(MinecraftClient.getInstance().player).inventory.getCursorStack().isEmpty())
		{
			InfoUtils.printActionbarMessage("Uncleaned inventory");
			return false;
		}
		if (!hasEnoughItemsInInventory(offer.getAdjustedFirstBuyItem()))
		{
			InfoUtils.printActionbarMessage("Not enough money " + offer.getAdjustedFirstBuyItem());
			return false;
		}
		if (!hasEnoughItemsInInventory(offer.getSecondBuyItem()))
		{
			InfoUtils.printActionbarMessage("Not enough money " + offer.getSecondBuyItem());
			return false;
		}
		if (!canReceiveOutput(offer.getSellItem()))
		{
			InfoUtils.printActionbarMessage("Not enough space for output");
			return false;
		}
		return true;
	}

	private boolean inputSlotsAreEmpty()
	{
		return this.container.getSlot(0).getStack().isEmpty()
				&& this.container.getSlot(1).getStack().isEmpty()
				&& this.container.getSlot(2).getStack().isEmpty();
	}

	private boolean hasEnoughItemsInInventory(ItemStack stack)
	{
		int remaining = stack.getCount();
		for (int i = container.slots.size() - 36; i < this.container.slots.size(); i++)
		{
			ItemStack invstack = this.container.getSlot(i).getStack();
			if (invstack == null)
				continue;
			if (areItemStacksMergable(stack, invstack))
			{
				//System.out.println("taking "+invstack.getCount()+" items from slot # "+i);
				remaining -= invstack.getCount();
			}
			if (remaining <= 0)
				return true;
		}
		return false;
	}

	private boolean canReceiveOutput(ItemStack stack)
	{
		int remaining = stack.getCount();
		for (int i = this.container.slots.size() - 36; i < this.container.slots.size(); i++)
		{
			ItemStack invstack = this.container.getSlot(i).getStack();
			if (invstack == null || invstack.isEmpty())
			{
				//System.out.println("can put result into empty slot "+i);
				return true;
			}
			if (areItemStacksMergable(stack, invstack)
					&& stack.getMaxCount() >= stack.getCount() + invstack.getCount())
			{
				//System.out.println("Can merge "+(invstack.getMaxStackSize()-invstack.getCount())+" items with slot "+i);
				remaining -= (invstack.getMaxCount() - invstack.getCount());
			}
			if (remaining <= 0)
				return true;
		}
		return false;
	}

	private void transact(TradeOffer offer)
	{
		//System.out.println("fill input slots called");
		int putback0, putback1 = -1;
		putback0 = fillSlot(0, offer.getAdjustedFirstBuyItem());
		putback1 = fillSlot(1, offer.getSecondBuyItem());

		getslot(2, offer.getSellItem(), putback0, putback1);
		//System.out.println("putting back to slot "+putback0+" from 0, and to "+putback1+"from 1");
		if (putback0 != -1)
		{
			slotClick(0);
			slotClick(putback0);
		}
		if (putback1 != -1)
		{
			slotClick(1);
			slotClick(putback1);
		}
	}

	/**
	 * @param slot  - the number of the (trading) slot that should receive items
	 * @param stack - what the trading slot should receive
	 * @return the number of the inventory slot into which these items should be put back
	 * after the transaction. May be -1 if nothing needs to be put back.
	 */
	private int fillSlot(int slot, ItemStack stack)
	{
		int remaining = stack.getCount();
		for (int i = this.container.slots.size() - 36; i < this.container.slots.size(); i++)
		{
			ItemStack invstack = this.container.getSlot(i).getStack();
			if (invstack == null)
				continue;
			boolean needPutBack = false;
			if (areItemStacksMergable(stack, invstack))
			{
				if (stack.getCount() + invstack.getCount() > stack.getMaxCount())
					needPutBack = true;
				remaining -= invstack.getCount();
				// System.out.println("taking "+invstack.getCount()+" items from slot # "+i+", remaining is now "+remaining);
				slotClick(i);
				slotClick(slot);
			}
			if (needPutBack)
			{
				slotClick(i);
			}
			if (remaining <= 0)
				return remaining < 0 ? i : -1;
		}
		// We should not be able to arrive here, since hasEnoughItemsInInventory should have been
		// called before fillSlot. But if we do, something went wrong; in this case better do a bit less.
		return -1;
	}

	private boolean areItemStacksMergable(ItemStack a, ItemStack b)
	{
		if (a == null || b == null)
			return false;
		if (a.getItem() == b.getItem()
				&& (!a.isDamageable() || a.getDamage() == b.getDamage())
				&& ItemStack.areTagsEqual(a, b))
			return true;
		return false;
	}

	private void getslot(int slot, ItemStack stack, int... forbidden)
	{
		int remaining = stack.getCount();
		slotClick(slot);
		for (int i = this.container.slots.size() - 36; i < this.container.slots.size(); i++)
		{
			ItemStack invstack = this.container.getSlot(i).getStack();
			if (invstack == null || invstack.isEmpty())
			{
				continue;
			}
			if (areItemStacksMergable(stack, invstack)
					&& invstack.getCount() < invstack.getMaxCount()
			)
			{
				// System.out.println("Can merge "+(invstack.getMaxStackSize()-invstack.getCount())+" items with slot "+i);
				remaining -= (invstack.getMaxCount() - invstack.getCount());
				slotClick(i);
			}
			if (remaining <= 0)
				return;
		}

		// When looking for an empty slot, don't take one that we want to put some input back to.
		for (int i = this.container.slots.size() - 36; i < this.container.slots.size(); i++)
		{
			boolean isForbidden = false;
			for (int f : forbidden)
			{
				if (i == f)
				{
					isForbidden = true;
					break;
				}
			}
			if (isForbidden)
				continue;
			ItemStack invstack = this.container.getSlot(i).getStack();
			if (invstack == null || invstack.isEmpty())
			{
				slotClick(i);
				// System.out.println("putting result into empty slot "+i);
				return;
			}
		}
	}

	private void slotClick(int slot)
	{
		((ContainerScreenAccessor)this.merchantScreen).invokeOnMouseClick(null, slot, 0, SlotActionType.PICKUP);
	}
}

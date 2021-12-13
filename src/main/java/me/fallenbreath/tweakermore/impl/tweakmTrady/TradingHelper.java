package me.fallenbreath.tweakermore.impl.tweakmTrady;

import fi.dy.masa.malilib.util.InfoUtils;
import me.fallenbreath.tweakermore.config.TweakerMoreToggles;
import me.fallenbreath.tweakermore.mixins.tweaks.tweakmTradyLapis.ContainerScreenAccessor;
import me.fallenbreath.tweakermore.mixins.tweaks.tweakmTradyLapis.MerchantScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.container.MerchantContainer;
import net.minecraft.container.SlotActionType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TraderOfferList;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class TradingHelper
{
	private final MerchantScreen merchantScreen;
	private final MerchantContainer container;
	@Nullable
	private TradeInfo tradeInfo;

	public TradingHelper(MerchantScreen merchantScreen)
	{
		this.merchantScreen = merchantScreen;
		this.container = merchantScreen.getContainer();
	}

	public int getContainerId()
	{
		return this.container.syncId;
	}

	private static final ItemStack EMERALD_1x = new ItemStack(Items.EMERALD, 1);
	private static final ItemStack LAPIS_LAZULI_2x = new ItemStack(Items.LAPIS_LAZULI, 2);
	private static final ItemStack EMPTY = ItemStack.EMPTY;

	private static boolean offerMatches(TradeOffer offer, Object buy1, Object buy2, Object sell)
	{
		BiFunction<Object, ItemStack, Boolean> tester = (current, excepted) ->
		{
			if (excepted.isEmpty())
			{
				return current instanceof ItemStack && ((ItemStack)current).isEmpty();
			}
			else if (current instanceof ItemStack)
			{
				if (((ItemStack)current).isEmpty())
				{
					return false;
				}
				return ItemStack.areItemsEqual((ItemStack)current, excepted);
			}
			else if (current instanceof Item)
			{
				return excepted.getItem() == current;
			}
			throw new RuntimeException("Unexpected testing object class: " + current.getClass());
		};
		return tester.apply(buy1, offer.getAdjustedFirstBuyItem()) && tester.apply(buy2, offer.getSecondBuyItem()) && tester.apply(sell, offer.getSellItem());
	}

	public void process()
	{
		if (TweakerMoreToggles.TWEAKM_TRADY_LAPIS.getBooleanValue())
		{
			TraderOfferList traderOfferList = this.container.getRecipes();
			int rottenFreshIdx = -1;
			int lapisLazuliIdx = -1;
			int redstoneIdx = -1;
			boolean hasLapis = false;
			for (int i = 0; i < traderOfferList.size(); i++)
			{
				TradeOffer tradeOffer = traderOfferList.get(i);
				boolean isLapis = offerMatches(tradeOffer, EMERALD_1x, EMPTY, LAPIS_LAZULI_2x);
				hasLapis |= isLapis;
				if (tradeOffer.isDisabled())
				{
					continue;
				}
				if (isLapis)
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
			if (lapisLazuliIdx != -1)
			{
				this.prepareTrade(lapisLazuliIdx, true);
			}
			else if (rottenFreshIdx != -1 && !hasLapis)
			{
				this.prepareTrade(rottenFreshIdx, false);
			}
			else if (redstoneIdx != -1)
			{
				this.prepareTrade(redstoneIdx, false);
			}
			else
			{
				InfoUtils.printActionbarMessage("It's useless now, unless it resets");
				closeContainer();
			}
		}
	}

	private void closeContainer()
	{
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player != null)
		{
			player.closeContainer();
		}
	}

	private void prepareTrade(int offerIndex, boolean tradeAll)
	{
		// select slot
		((MerchantScreenAccessor)this.merchantScreen).setSelectedIndex(offerIndex);
		((MerchantScreenAccessor)this.merchantScreen).invokeSyncRecipeIndex();
		// record trade
		// viaversion will make the server send an inventory update packet after the client sends the offer index
		// so we will do the trade when the inventory packet is received
		this.tradeInfo = new TradeInfo(offerIndex, tradeAll);
	}

	public void doTrade()
	{
		if (this.tradeInfo == null)
		{
			return;
		}
		TradeOffer offer = this.container.getRecipes().get(this.tradeInfo.offerIndex);
		int counter = 0;
		while (counter++ < 100)
		{
			if (!canTrade(offer, counter == 1))
			{
				break;
			}
			System.out.println("trade try #" + counter);
			this.transact(offer);

			if (!this.tradeInfo.tradeAll)
			{
				break;
			}
		}
	}

	//////////////////////////////
	//  Trading Implementation  //
	//////////////////////////////

	private boolean canTrade(TradeOffer offer, boolean doLog)
	{
		Consumer<String> log = msg -> {
			if (doLog)
			{
				InfoUtils.printActionbarMessage(msg);
			}
		};
		if (offer.isDisabled())
		{
			log.accept("Offer disabled");
			return false;
		}
		if (!this.inputSlotsAreEmpty() || !Objects.requireNonNull(MinecraftClient.getInstance().player).inventory.getCursorStack().isEmpty())
		{
			log.accept("Uncleaned inventory");
			return false;
		}
		if (!hasEnoughItemsInInventory(offer.getAdjustedFirstBuyItem()))
		{
			log.accept("Not enough money " + offer.getAdjustedFirstBuyItem());
			return false;
		}
		if (!hasEnoughItemsInInventory(offer.getSecondBuyItem()))
		{
			log.accept("Not enough money " + offer.getSecondBuyItem());
			return false;
		}
		if (!canReceiveOutput(offer.getSellItem()))
		{
			log.accept("Not enough space for output");
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
		if (remaining == 0)
		{
			return true;
		}
		for (int i = container.slots.size() - 36; i < this.container.slots.size(); i++)
		{
			ItemStack invstack = this.container.getSlot(i).getStack();
			if (invstack == null)
				continue;
			if (areItemStacksMergable(stack, invstack))
			{
				System.out.println("[hasEnough] taking "+invstack.getCount()+" items from slot # "+i);
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
				System.out.println("can put result into empty slot "+i);
				return true;
			}
			if (areItemStacksMergable(stack, invstack)
					&& stack.getMaxCount() >= stack.getCount() + invstack.getCount())
			{
				System.out.println("Can merge "+(invstack.getMaxCount()-invstack.getCount())+" items with slot "+i);
				remaining -= (invstack.getMaxCount() - invstack.getCount());
			}
			if (remaining <= 0)
				return true;
		}
		return false;
	}

	private void transact(TradeOffer offer)
	{
		System.out.println("fill input slots called");
		int putback0, putback1 = -1;
		putback0 = fillSlot(0, offer.getAdjustedFirstBuyItem());
		putback1 = fillSlot(1, offer.getSecondBuyItem());

		getslot(2, offer.getSellItem(), putback0, putback1);
		System.out.println("putting back to slot "+putback0+" from 0, and to "+putback1+"from 1");
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
		if (stack.isEmpty())
		{
			return -1;
		}
		int remaining = stack.getCount();
		for (int i = this.container.slots.size() - 36; i < this.container.slots.size(); i++)
		{
			ItemStack invstack = this.container.getSlot(i).getStack();
			boolean needPutBack = false;
			if (areItemStacksMergable(stack, invstack))
			{
				if (stack.getCount() + invstack.getCount() > stack.getMaxCount())
					needPutBack = true;
				remaining -= invstack.getCount();
				System.out.println("[fillSlot] taking "+invstack.getCount()+" items from slot # "+i+", remaining is now "+remaining);
				slotClick(i);
				slotClick(slot);
			}
			if (needPutBack)
			{
			//	slotClick(i);
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
				System.out.println("Can merge "+(invstack.getMaxCount()-invstack.getCount())+" items with slot "+i);
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
				System.out.println("putting result into empty slot "+i);
				return;
			}
		}
	}

	private void slotClick(int slot)
	{
		System.out.println("slotClick "+slot);
		((ContainerScreenAccessor)this.merchantScreen).invokeOnMouseClick(null, slot, 0, SlotActionType.PICKUP);
	}

	private static class TradeInfo
	{
		public final int offerIndex;
		public final boolean tradeAll;

		private TradeInfo(int offerIndex, boolean tradeAll)
		{
			this.offerIndex = offerIndex;
			this.tradeAll = tradeAll;
		}
	}
}

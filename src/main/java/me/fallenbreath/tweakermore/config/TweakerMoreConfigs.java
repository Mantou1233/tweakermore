package me.fallenbreath.tweakermore.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.*;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.restrictions.ItemRestriction;
import fi.dy.masa.malilib.util.restrictions.UsageRestriction;
import me.fallenbreath.tweakermore.TweakerMoreMod;
import me.fallenbreath.tweakermore.gui.TweakermoreConfigGui;
import me.fallenbreath.tweakermore.impl.copySignTextToClipBoard.SignTextCopier;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Util;
import net.minecraft.util.registry.Registry;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TweakerMoreConfigs
{
	@Config(Config.Type.GENERIC)
	public static final ConfigInteger AUTO_FILL_CONTAINER_THRESHOLD = new ConfigInteger("autoFillContainerThreshold", 2, 1, 36, "autoFillContainerThreshold.comment");
	@Config(Config.Type.GENERIC)
	public static final ConfigDouble NETHER_PORTAL_SOUND_CHANCE = new ConfigDouble("netherPortalSoundChance", 0.01D, 0.0D, 0.01D, "netherPortalSoundChance.comment");
	@Config(Config.Type.GENERIC)
	public static final ConfigBoolean VILLAGER_OFFER_USES_DISPLAY = new ConfigBoolean("villagerOfferUsesDisplay", false, "villagerOfferUsesDisplay.comment");
	@Config(Config.Type.GENERIC)
	public static final ConfigBoolean SHULKER_TOOLTIP_ENCHANTMENT_HINT = new ConfigBoolean("shulkerTooltipEnchantmentHint", false, "shulkerTooltipEnchantmentHint.comment");

	@Config(Config.Type.HOTKEY)
	public static final ConfigHotkey COPY_SIGN_TEXT_TO_CLIPBOARD = new ConfigHotkey("copySignTextToClipBoard", "", "copySignTextToClipBoard.comment");

	@Config(Config.Type.LIST)
	public static final ConfigOptionList HAND_RESTORE_LIST_TYPE = new ConfigOptionList("handRestockListType", UsageRestriction.ListType.NONE, "handRestockListType.comment");
	@Config(Config.Type.LIST)
	public static final ConfigStringList HAND_RESTORE_WHITELIST = new ConfigStringList("handRestockWhiteList", ImmutableList.of(getItemId(Items.BUCKET)), "handRestockWhiteList.comment");
	@Config(Config.Type.LIST)
	public static final ConfigStringList HAND_RESTORE_BLACKLIST = new ConfigStringList("handRestockBlackList", ImmutableList.of(getItemId(Items.LAVA_BUCKET)), "handRestockBlackList.comment");
	public static final ItemRestriction HAND_RESTORE_RESTRICTION = new ItemRestriction();

	@Config(Config.Type.DISABLE)
	public static final ConfigBooleanHotkeyed DISABLE_LIGHT_UPDATES = new ConfigBooleanHotkeyed("disableLightUpdates", false, "", "disableLightUpdates.comment", "Disable Light Updates");
	@Config(Config.Type.DISABLE)
	public static final ConfigBooleanHotkeyed DISABLE_REDSTONE_WIRE_PARTICLE = new ConfigBooleanHotkeyed("disableRedstoneWireParticle", false, "", "disableRedstoneWireParticle.comment", "Disable particle of redstone wire");

	@Config(Config.Type.CONFIG)
	public static final ConfigHotkey OPEN_TWEAKERMORE_CONFIG_GUI = new ConfigHotkey("openTweakermoreConfigGui", "", "openTweakermoreConfigGui.comment");

	// trady
	@Config(Config.Type.GENERIC)
	public static final ConfigBoolean TRADY_THROW_IF_FULL = new ConfigBoolean("tradyThrowIfFull", false, "tradyThrowIfFull.comment");
	@Config(Config.Type.LIST)
	public static final ConfigStringList TRADY_FARMER_TARGETS = new ConfigStringList("tradyFarmerTargets", ImmutableList.of(getItemId(Items.CARROT), getItemId(Items.POTATO), getItemId(Items.PUMPKIN)), "tradyFarmerTargets.comment");

	private static String getItemId(Item item)
	{
		return Registry.ITEM.getId(item).toString();
	}

	public static void initCallbacks()
	{
		TweakerMoreConfigs.COPY_SIGN_TEXT_TO_CLIPBOARD.getKeybind().setCallback(SignTextCopier::copySignText);
		TweakerMoreConfigs.OPEN_TWEAKERMORE_CONFIG_GUI.getKeybind().setCallback((action, key) -> {
			GuiBase.openGui(new TweakermoreConfigGui());
			return true;
		});
	}

	/**
	 * ============================
	 *    Implementation Details
	 * ============================
	 */

	private static final Map<Config.Type, List<IConfigBase>> OPTION_SETS = Util.make(() -> {
		HashMap<Config.Type, List<IConfigBase>> map = Maps.newHashMap();
		map.put(Config.Type.TWEAK, new ArrayList<>(TweakerMoreToggles.getFeatureToggles()));
		for (Field field : TweakerMoreConfigs.class.getDeclaredFields())
		{
			Config annotation = field.getAnnotation(Config.class);
			if (annotation != null)
			{
				try
				{
					IConfigBase option = (IConfigBase)field.get(null);
					for (Config.Type type : annotation.value())
					{
						map.computeIfAbsent(type, key -> Lists.newArrayList()).add(option);
					}
				}
				catch (IllegalAccessException e)
				{
					e.printStackTrace();
				}
			}
		}
		return map;
	});

	@SuppressWarnings("unchecked")
	public static <T extends IConfigBase> List<T> getOptions(Config.Type optionType)
	{
		return (List<T>)OPTION_SETS.getOrDefault(optionType, Lists.newArrayList());
	}

	public static List<IConfigBase> getOptions(Predicate<Config.Type> predicate)
	{
		return OPTION_SETS.keySet().stream().
				filter(predicate).
				map(OPTION_SETS::get).
				flatMap(Collection::stream).
				collect(Collectors.toList());
	}

	public static <T extends IConfigBase> ImmutableList<T> updateOptionList(List<T> originalConfig, Config.Type optionType)
	{
		List<T> optionList = Lists.newArrayList(originalConfig);
		optionList.addAll(getOptions(optionType));
		return ImmutableList.copyOf(optionList);
	}

	private static final String CONFIG_FILE_NAME = TweakerMoreMod.MOD_ID + ".json";

	private static File getConfigFile()
	{
		return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME).toFile();
	}

	private static JsonObject ROOT_JSON_OBJ = new JsonObject();

	/**
	 * ====================
	 *    Config Storing
	 * ====================
	 */

	public static void loadFromFile()
	{
		File configFile = getConfigFile();
		if (configFile.exists() && configFile.isFile() && configFile.canRead())
		{
			JsonElement element = JsonUtils.parseJsonFile(configFile);

			if (element != null && element.isJsonObject())
			{
				JsonObject root = element.getAsJsonObject();

				ConfigUtils.readConfigBase(root, "Generic", getOptions(Config.Type.GENERIC));
				ConfigUtils.readConfigBase(root, "GenericHotkeys", getOptions(Config.Type.HOTKEY));
				ConfigUtils.readConfigBase(root, "Lists", getOptions(Config.Type.LIST));
				ConfigUtils.readHotkeyToggleOptions(root, "TweakHotkeys", "TweakToggles", getOptions(Config.Type.TWEAK));
				ConfigUtils.readHotkeyToggleOptions(root, "DisableHotkeys", "DisableToggles", getOptions(Config.Type.DISABLE));
				ConfigUtils.readConfigBase(root, "Config", getOptions(Config.Type.CONFIG));

				ROOT_JSON_OBJ = root;
			}
		}
	}

	public static void saveToFile()
	{
		File configFile = getConfigFile();
		JsonObject root = ROOT_JSON_OBJ;

		ConfigUtils.writeConfigBase(root, "Generic", getOptions(Config.Type.GENERIC));
		ConfigUtils.writeConfigBase(root, "GenericHotkeys", getOptions(Config.Type.HOTKEY));
		ConfigUtils.writeConfigBase(root, "Lists", getOptions(Config.Type.LIST));
		ConfigUtils.writeHotkeyToggleOptions(root, "TweakHotkeys", "TweakToggles", getOptions(Config.Type.TWEAK));
		ConfigUtils.writeHotkeyToggleOptions(root, "DisableHotkeys", "DisableToggles", getOptions(Config.Type.DISABLE));
		ConfigUtils.writeConfigBase(root, "Config", getOptions(Config.Type.CONFIG));

		JsonUtils.writeJsonToFile(root, configFile);
	}
}

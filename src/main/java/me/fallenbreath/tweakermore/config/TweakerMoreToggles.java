package me.fallenbreath.tweakermore.config;

import com.chocohead.mm.api.ClassTinkerers;
import fi.dy.masa.tweakeroo.config.FeatureToggle;

public class TweakerMoreToggles
{
	public static final FeatureToggle TWEAKM_AUTO_CLEAN_CONTAINER = ClassTinkerers.getEnum(FeatureToggle.class, "TWEAKM_AUTO_CLEAN_CONTAINER");
	public static final FeatureToggle TWEAKM_AUTO_FILL_CONTAINER = ClassTinkerers.getEnum(FeatureToggle.class, "TWEAKM_AUTO_FILL_CONTAINER");
	public static final FeatureToggle TWEAKM_TRADY_LAPIS = ClassTinkerers.getEnum(FeatureToggle.class, "TWEAKM_TRADY_LAPIS");
	public static final FeatureToggle TWEAKM_TRADY_FARMER = ClassTinkerers.getEnum(FeatureToggle.class, "TWEAKM_TRADY_FARMER");
	public static final FeatureToggle TWEAKM_AUTO_PICK_SCHEMATIC_BLOCK = ClassTinkerers.getEnum(FeatureToggle.class, "TWEAKM_AUTO_PICK_SCHEMATIC_BLOCK");
}

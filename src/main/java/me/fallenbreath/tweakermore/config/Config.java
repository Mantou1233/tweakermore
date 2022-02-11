package me.fallenbreath.tweakermore.config;

import fi.dy.masa.malilib.interfaces.IStringValue;
import fi.dy.masa.malilib.util.StringUtils;
import me.fallenbreath.tweakermore.util.dependency.Strategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Config
{
	Type value();

	Category category() default Category.MC_TWEAKS;

	Strategy strategy() default @Strategy();

	enum Type implements IStringValue
	{
		GENERIC, HOTKEY, LIST, TWEAK, DISABLE;

		@Override
		public String getStringValue()
		{
			return StringUtils.translate("tweakermore.gui.config_type." + this.name().toLowerCase());
		}
	}

	enum Category
	{
		MC_TWEAKS, MOD_TWEAKS, TRADY, SETTING;

		public String getDisplayName()
		{
			return StringUtils.translate("tweakermore.gui.config_category." + this.name().toLowerCase());
		}
	}
}

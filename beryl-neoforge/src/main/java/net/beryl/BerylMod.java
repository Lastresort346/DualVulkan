package net.beryl;

import net.beryl.option.BerylOptions;
import net.beryl.option.Config;
import net.beryl.render.RenderingPipeline;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.vulkanmod.config.gui.ModSettingsEntry;
import net.vulkanmod.config.gui.ModSettingsRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

@Mod("beryl")
public class BerylMod {
    public static final Logger LOGGER = LogManager.getLogger("Beryl");
    public static Config CONFIG;

    public BerylMod() {
        LOGGER.info("== Beryl ==");
        Path configPath = FMLPaths.CONFIGDIR.get().resolve("beryl_settings.json");
        CONFIG = Config.load(configPath);
        if (CONFIG == null) { CONFIG = new Config(); CONFIG.write(); }

        ModSettingsEntry entry = new ModSettingsEntry(
                Component.literal("Beryl"),
                () -> ResourceLocation.fromNamespaceAndPath("beryl", "beryl_icon_transparent.png"),
                BerylOptions::getOptionPages,
                () -> CONFIG.write()
        );
        ModSettingsRegistry.INSTANCE.addModEntry(entry);
    }
}

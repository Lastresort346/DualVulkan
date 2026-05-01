package net.vulkanmod;

// Fabric API imports removed for NeoForge compatibility
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.vulkanmod.config.Config;
import net.vulkanmod.config.Platform;
import net.vulkanmod.config.UpdateChecker;
import net.vulkanmod.vulkan.device.MultiGPUWorkloadDistributor;
// FRAPI import removed - will be replaced with NeoForge equivalent
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

@Mod("vulkanmod")
public class Initializer {
	public static final Logger LOGGER = LogManager.getLogger("VulkanMod");

	private static String VERSION;
	public static Config CONFIG;

	// Fabric onInitializeClient removed - NeoForge uses constructor

	public Initializer() {
		initialize();
	}

	public static void initialize() {
		if (CONFIG != null) {
			return;
		}

		VERSION = ModList.get().getModContainerById("vulkanmod")
				.map(container -> container.getModInfo().getVersion().toString())
				.orElse("0.0.0");

		LOGGER.info("== VulkanMod ==");

		Platform.init();

		var configPath = FMLPaths.CONFIGDIR.get().resolve("vulkanmod_settings.json");

		CONFIG = loadConfig(configPath);

		// Initialize Multi-GPU Workload Distributor
		MultiGPUWorkloadDistributor.initialize();

//		try {
//			net.fabricmc.fabric.api.renderer.v1.RendererAccess.INSTANCE.registerRenderer(VulkanModRenderer.INSTANCE);
//		} catch (Exception e) {
//			LOGGER.warn("Failed to register VulkanMod FRAPI renderer (FRAPI stub mode)", e);
//		}

		UpdateChecker.checkForUpdates();
	}

	private static Config loadConfig(Path path) {
        return Config.load(path);
	}

	public static String getVersion() {
		return VERSION;
	}
}

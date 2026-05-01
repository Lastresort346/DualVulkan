package net.beryl.option;

import net.beryl.BerylMod;
import net.vulkanmod.config.gui.OptionBlock;
import net.vulkanmod.config.option.OptionPage;
import net.vulkanmod.config.option.RangeOption;
import net.vulkanmod.config.option.SwitchOption;
import net.minecraft.network.chat.Component;

import java.util.List;

public class BerylOptions {
    public static Config config;

    public static List<OptionPage> getOptionPages() {
        config = BerylMod.CONFIG;
        OptionBlock shadersBlock = new OptionBlock("Shaders", new net.vulkanmod.config.option.Option<?>[]{
                new SwitchOption(Component.translatable("beryl.options.shaders"),
                        v -> { config.shadersOn = v; net.beryl.render.RenderingPipeline.setUseShaderPipeline(v); },
                        () -> config.shadersOn),
                new RangeOption(Component.translatable("beryl.options.shadowDistance"),
                        4, 32, 2,
                        v -> config.shadowRenderDistance = v,
                        () -> config.shadowRenderDistance),
                new RangeOption(Component.translatable("beryl.options.shadowResolution"),
                        512, 4096, 512,
                        v -> { config.shadowResolution = v; net.beryl.render.RenderingPipeline.createShadowFramebuffers(); },
                        () -> config.shadowResolution)
        });
        return List.of(new OptionPage("Beryl", new OptionBlock[]{ shadersBlock }));
    }
}

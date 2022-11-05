package com.github.Soulphur0;

import com.github.Soulphur0.config.EanConfigFile;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("elytra_aeronautics")
public class ElytraAeronautics {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LogManager.getLogger("ElytraAeronautics");

	public ElytraAeronautics() {
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
		MinecraftForge.EVENT_BUS.register(this);
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, EanConfigFile.CLIENT_SPEC);
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EanConfigFile.COMMON_SPEC);
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

	}

	private void setup(FMLCommonSetupEvent event) {
		LOGGER.info("Elytra Aeronautics initialized! Have a good flight!");
	}

}

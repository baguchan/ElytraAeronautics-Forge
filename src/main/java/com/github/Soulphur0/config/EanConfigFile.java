package com.github.Soulphur0.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

@Mod.EventBusSubscriber(modid = "elytra_aeronautics", bus = Mod.EventBusSubscriber.Bus.MOD)
public class EanConfigFile {
    public static final Client CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Common COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;

    static {
        Pair<Client, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC = specPair.getRight();
        CLIENT = specPair.getLeft();
        Pair<Common, ForgeConfigSpec> specPair2 = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = specPair2.getRight();
        COMMON = specPair2.getLeft();
    }

    public static class Common {
        public final ForgeConfigSpec.BooleanValue altitudeDeterminesSpeed;
        public final ForgeConfigSpec.DoubleValue minSpeed;
        public final ForgeConfigSpec.DoubleValue maxSpeed;
        public final ForgeConfigSpec.DoubleValue curveStart;
        public final ForgeConfigSpec.DoubleValue curveEnd;
        public final ForgeConfigSpec.BooleanValue sneakRealignsPitch;

        public final ForgeConfigSpec.ConfigValue<Float> realignmentAngle;
        public final ForgeConfigSpec.ConfigValue<Float> realignmentRate;


        public Common(ForgeConfigSpec.Builder builder) {
            altitudeDeterminesSpeed = builder
                    .comment("altitudeDeterminesSpeed")
                    .define("altitudeDeterminesSpeed", true);
            minSpeed = builder
                    .comment("minSpeed")
                    .defineInRange("minSpeed", 30.35D, 0D, 1000D);
            maxSpeed = builder
                    .comment("maxSpeed")
                    .defineInRange("maxSpeed", 257.22D, 0D, 1000D);
            curveStart = builder
                    .comment("carveStart")
                    .defineInRange("carveStart", 250.0D, 0D, 10000D);
            curveEnd = builder
                    .comment("carveEnd")
                    .defineInRange("carveEnd", 1000.22D, 0D, 10000D);
            sneakRealignsPitch = builder
                    .comment("sneakRealignsPitch")
                    .define("sneakRealignsPitch", true);
            realignmentAngle = builder
                    .comment("realignmentAngle")
                    .define("realignmentAngle", 0.0F);
            realignmentRate = builder
                    .comment("realignmentRate")
                    .define("realignmentRate", 0.1F);
        }
    }

    public static class Client {

        public static final List<CloudLayer> defaultCloudList = Lists.newArrayList();
        public final ForgeConfigSpec.IntValue layerAmount;

        public final ForgeConfigSpec.ConfigValue<Float> layerDistance;
        public final ForgeConfigSpec.ConfigValue<Float> stackingAltitude;

        public final ForgeConfigSpec.ConfigValue<String> cloudType;
        public final ForgeConfigSpec.ConfigValue<String> renderMode;
        public final ForgeConfigSpec.ConfigValue<String> lodRenderMode;

        public final ForgeConfigSpec.BooleanValue useSmoothLODs;

        public Client(ForgeConfigSpec.Builder builder) {
            layerAmount = builder
                    .comment("layerAmount")
                    .defineInRange("layerAmount", 2, 0, 10);
            layerDistance = builder
                    .comment("layerDistance")
                    .define("layerDistance", 250.0F);
            stackingAltitude = builder
                    .comment("stackingAltitude")
                    .define("stackingAltitude", 192.0F);
            cloudType = builder
                    .comment("set cloudTypes[LOD, FANCY, FAST]")
                    .define("cloudType", CloudTypes.LOD.name(), (name) -> {
                        return name != null && (name.equals(CloudTypes.LOD.name()) || name.equals(CloudTypes.FANCY.name()) || name.equals(CloudTypes.FAST.name()));
                    });
            renderMode = builder
                    .comment("set renderMode[TWO_IN_ADVANCE, ALWAYS_RENDER, ONE_IN_ADVANCE]")
                    .define("renderMode", CloudRenderModes.ALWAYS_RENDER.name(), (name) -> {
                        return name != null && (name.equals(CloudRenderModes.TWO_IN_ADVANCE.name()) || name.equals(CloudRenderModes.ALWAYS_RENDER.name()) || name.equals(CloudRenderModes.ONE_IN_ADVANCE.name()));
                    });
            lodRenderMode = builder
                    .comment("set lodRenderMode [TWO_IN_ADVANCE, ALWAYS_RENDER, ONE_IN_ADVANCE]")
                    .define("lodRenderMode", CloudRenderModes.TWO_IN_ADVANCE.name(), (name) -> {
                        return name != null && (name.equals(CloudRenderModes.TWO_IN_ADVANCE.name()) || name.equals(CloudRenderModes.ALWAYS_RENDER.name()) || name.equals(CloudRenderModes.ONE_IN_ADVANCE.name()));
                    });
            useSmoothLODs = builder
                    .comment("useSmoothLODs")
                    .define("useSmoothLODs", false);

        }
    }

    private static boolean isValidCloudLayer(Object object) {
        return true;
    }

    // ? Getters and setters
    public static int getLayerAmount() {
        return CLIENT.layerAmount.get();
    }

    public static List<CloudLayer> getCloudLayerList() {
        return Client.defaultCloudList;
    }

    public static float getLayerDistance() {
        return CLIENT.layerDistance.get();
    }

    public static CloudTypes getCloudType() {
        return CloudTypes.valueOf(CLIENT.cloudType.get());
    }

    public static CloudRenderModes getRenderMode() {
        return CloudRenderModes.valueOf(CLIENT.renderMode.get());
    }

    public static CloudRenderModes getLodRenderMode() {
        return CloudRenderModes.valueOf(CLIENT.lodRenderMode.get());
    }

    public static double getMinSpeed() {
        return COMMON.minSpeed.get();
    }

    public static double getMaxSpeed() {
        return COMMON.maxSpeed.get();
    }

    public static double getCurveStart() {
        return COMMON.curveStart.get();
    }

    public static double getCurveEnd() {
        return COMMON.curveEnd.get();
    }

    public static boolean isSneakRealignsPitch() {
        return COMMON.sneakRealignsPitch.get();
    }

    public static float getRealignmentRate() {
        return COMMON.realignmentRate.get();
    }

    public static float getRealignmentAngle() {
        return COMMON.realignmentAngle.get();
    }

    public static float getStackingAltitude() {
        return CLIENT.stackingAltitude.get();
    }

    public static boolean isUseSmoothLODs() {
        return CLIENT.useSmoothLODs.get();
    }

    public static boolean isAltitudeDeterminesSpeed() {
        return COMMON.altitudeDeterminesSpeed.get();
    }

    // ? Preset setup method

    @SubscribeEvent
    public static void defaultPreset(ModConfigEvent.Loading event) {
        EanConfigFile.getCloudLayerList().clear();

        if (EanConfigFile.getLayerAmount() > 0) {
            for (int i = 0; i < EanConfigFile.getLayerAmount(); i++) {
                Client.defaultCloudList.add(new CloudLayer(EanConfigFile.getStackingAltitude() + EanConfigFile.getLayerDistance() * i, CloudTypes.LOD, CloudRenderModes.ALWAYS_RENDER, 0.0F, CloudRenderModes.ONE_IN_ADVANCE, 0.0F, false));
            }
        }

        for (CloudLayer layer : getCloudLayerList()) {
            layer.setCloudType(getCloudType());
            layer.setRenderMode(getRenderMode());
            layer.setLodRenderMode(getLodRenderMode());
        }
    }


    @SubscribeEvent
    public static void defaultPreset(ModConfigEvent.Reloading event) {
        EanConfigFile.getCloudLayerList().clear();

        if (EanConfigFile.getLayerAmount() > 0) {
            for (int i = 0; i < EanConfigFile.getLayerAmount(); i++) {
                Client.defaultCloudList.add(new CloudLayer(EanConfigFile.getStackingAltitude() + EanConfigFile.getLayerDistance() * i, CloudTypes.LOD, CloudRenderModes.ALWAYS_RENDER, 0.0F, CloudRenderModes.ONE_IN_ADVANCE, 0.0F, false));
            }
        }

        for (CloudLayer layer : getCloudLayerList()) {
            layer.setCloudType(getCloudType());
            layer.setRenderMode(getRenderMode());
            layer.setLodRenderMode(getLodRenderMode());
        }
    }

}

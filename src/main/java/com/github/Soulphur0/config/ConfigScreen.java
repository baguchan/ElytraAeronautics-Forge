package com.github.Soulphur0.config;

import com.github.Soulphur0.ElytraAeronautics;
import me.shedaniel.clothconfig2.api.*;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.TranslatableText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigScreen {

    // Get current screen for the builder
    Screen screen = MinecraftClient.getInstance().currentScreen;

    // Screen builder objects
    ConfigBuilder builder = ConfigBuilder.create().setParentScreen(screen).setTitle(new TranslatableText("Elytra Aeronautics Configuration Screen"));
    ConfigEntryBuilder entryBuilder = builder.entryBuilder();

    // Config categories
    ConfigCategory elytraFlightSettings = builder.getOrCreateCategory(new TranslatableText("Elytra flight settings"));
    ConfigCategory cloudSettings = builder.getOrCreateCategory(new TranslatableText("Cloud settings"));

    // * Config file
    static public EanConfigFile eanConfigFile;

    public Screen buildScreen(){
        // ? Get config file from config folder
        eanConfigFile = ConfigFileReader.getConfigFile();

        // ? Build elytra flight category
        buildElytraFlightCategory();

        // ? Build cloud configuration category
        buildCloudCategory(eanConfigFile);

        // ? Set what to do when the config screen is saved.
        builder.setSavingRunnable(() ->{
            ConfigFileWriter.writeToFile(eanConfigFile);
            ElytraAeronautics.readConfigFileCue_WorldRendererMixin = true;
            ElytraAeronautics.readConfigFileCue_LivingEntityMixin = true;
        });

        // ? Return the screen to show.
        return builder.build();
    }

    // * [Elytra flight] methods and variables
    private void buildElytraFlightCategory(){
        elytraFlightSettings.addEntry(entryBuilder.startDoubleField(new TranslatableText("Flight speed constant " + "(Estimated max speed at max altitude and 0 degree pitch = "+ Math.floor(31.7966+0.560503*Math.exp(679.292*eanConfigFile.getSpeedConstantAdditionalValue())) +"m/s)"), eanConfigFile.getSpeedConstantAdditionalValue())
                .setDefaultValue(0.0088D)
                .setTooltip(new TranslatableText("WARNINIG: Proceed with caution as even the slightest increase to this constant may cause flight speed to rapidly spike up! Re-enter this menu to update the maximum speed estimate shown in this label."))
                .setSaveConsumer(newValue ->{
                    eanConfigFile.setSpeedConstantAdditionalValue(newValue);
                })
                .build());

        elytraFlightSettings.addEntry(entryBuilder.startTextDescription(new TranslatableText("--- Speed curve settings ---"))
                .setTooltip(new TranslatableText("Please note that each of the following values must be greater than the previous one (in ascending order)."))
                .build());

        elytraFlightSettings.addEntry(entryBuilder.startDoubleField(new TranslatableText("Flight speed curve beginning"), eanConfigFile.getCurveStart())
                .setDefaultValue(0.0D)
                .setMax(eanConfigFile.getCurveMiddle())
                .setTooltip(new TranslatableText("Altitude at which flight speed start to increase very very slightly."))
                .setSaveConsumer(newValue ->{
                    eanConfigFile.setCurveStart(newValue);
                })
                .build());

        elytraFlightSettings.addEntry(entryBuilder.startDoubleField(new TranslatableText("Flight speed curve middle point"), eanConfigFile.getCurveMiddle())
                .setDefaultValue(250.0D)
                .setMin(eanConfigFile.getCurveStart())
                .setMax(eanConfigFile.getCurveEnd())
                .setTooltip(new TranslatableText("Altitude at which flight speed starts to increase exponentially."))
                .setSaveConsumer(newValue ->{
                    eanConfigFile.setCurveMiddle(newValue);
                })
                .build());

        elytraFlightSettings.addEntry(entryBuilder.startDoubleField(new TranslatableText("Flight speed curve end"), eanConfigFile.getCurveEnd())
                .setDefaultValue(1000.0D)
                .setMin(eanConfigFile.getCurveMiddle())
                .setTooltip(new TranslatableText("Altitude at which flight speed stops to increase (maximum flight speed is achieved)."))
                .setSaveConsumer(newValue ->{
                    eanConfigFile.setCurveEnd(newValue);
                })
                .build());
    }

    // * [Cloud category] methods and variables
    private void buildCloudCategory(EanConfigFile eanConfigFile){
        // ? Get cloud layer list
        List<CloudLayer> cloudLayerList = eanConfigFile.getCloudLayerList();

        // -- GUI SETUP --
        // ? Variables
        List<AbstractConfigListEntry> layerList = new ArrayList<>(); // Used to store all layer subcategory menus.
        List<List<AbstractConfigListEntry>> layerAttributesList = new ArrayList<>(); // Used to store each layer subcategory menus' attributes.

        // ? Presets
        cloudSettings.addEntry(entryBuilder.startBooleanToggle(new TranslatableText("Reset to default preset?"), false)
                .setDefaultValue(true).setTooltip(new TranslatableText("By resetting to the default preset, cloud layers will be placed at the flight-speed-curve's middle and highest points."))
                .setSaveConsumer(newValue->{
                    if (newValue){
                        cloudLayerList.clear();
                        eanConfigFile.setLayerAmount(2);
                        cloudLayerList.add(new CloudLayer((float)eanConfigFile.getCurveMiddle(),CloudTypes.LOD,CloudRenderModes.ALWAYS_RENDER,0, CloudRenderModes.TWO_IN_ADVANCE,0));
                        cloudLayerList.add(new CloudLayer((float)eanConfigFile.getCurveEnd(),CloudTypes.LOD,CloudRenderModes.ALWAYS_RENDER,0, CloudRenderModes.TWO_IN_ADVANCE,0));
                    }
                })
                .build());

        // ? General settings
        cloudSettings.addEntry(entryBuilder.startTextDescription(new TranslatableText("--- General settings ---"))
                .setTooltip(new TranslatableText("Settings applied to all cloud layers at once!"))
                .build());

        // Layer amount field.
        cloudSettings.addEntry(entryBuilder.startIntField(new TranslatableText("Cloud layer amount"), eanConfigFile.getLayerAmount())
                .setTooltip(new TranslatableText("This value determines the amount of cloud layers there are. Besides the vanilla clouds. To make new layers show up in the config, re-enter the menu without restarting. For changes to apply in-game however, restarting is necessary."))
                .setDefaultValue(2)
                .setSaveConsumer(newValue -> {
                    // Save layer amount in class field and add all new layers (or remove exceeding ones) from the cloud layer list upon save.
                    eanConfigFile.setLayerAmount(newValue);
                    updateLayerListEntries(cloudLayerList, -1);
                })
                .build());

        // Distance between layers.
        cloudSettings.addEntry(entryBuilder.startFloatField(new TranslatableText("Distance between cloud layers"), eanConfigFile.getLayerDistance())
                        .setTooltip(new TranslatableText("When this value is changed, all cloud layers are relocated, and, starting from the altitude specified in the field below; they are placed on top of each other separated by this distance."))
                .setDefaultValue(250.0F)
                .setSaveConsumer(newValue -> {
                    if (newValue != eanConfigFile.getLayerDistance()){
                        eanConfigFile.setLayerDistance(newValue);
                        updateLayerListEntries(cloudLayerList, 0);
                    }
                })
                .build());

        // Cloud stacking start altitude
        cloudSettings.addEntry(entryBuilder.startFloatField(new TranslatableText("Cloud layers' lowest altitude"), eanConfigFile.getStackingAltitude())
                .setTooltip(new TranslatableText("When the distance between clouds is modified, clouds will re-stacked on top of each other starting at the altitude specified in this field."))
                .setDefaultValue(192.0F)
                .setSaveConsumer(newValue -> {
                    if (newValue != eanConfigFile.getStackingAltitude()){
                        eanConfigFile.setStackingAltitude(newValue);
                        updateLayerListEntries(cloudLayerList, 0);
                    }
                })
                .build());

        // Cloud type
        cloudSettings.addEntry(entryBuilder
                .startDropdownMenu(
                        new TranslatableText("Cloud type"),
                        DropdownMenuBuilder.TopCellElementBuilder.of(eanConfigFile.getCloudType(), value ->
                                switch (value) {
                                    case "FAST" -> CloudTypes.FAST;
                                    case "FANCY" -> CloudTypes.FANCY;
                                    default -> CloudTypes.LOD;
                                }),
                        DropdownMenuBuilder.CellCreatorBuilder.of(value ->
                        {
                            if (CloudTypes.LOD.equals(value)) {
                                return new TranslatableText("LOD");
                            } else if (CloudTypes.FAST.equals(value)) {
                                return new TranslatableText("FAST");
                            } else if (CloudTypes.FANCY.equals(value)) {
                                return new TranslatableText("FANCY");
                            }
                            return new TranslatableText("UNKNOWN CLOUD TYPE");
                        }))
                .setDefaultValue(CloudTypes.LOD)
                .setSuggestionMode(false)
                .setSelections(Arrays.asList(CloudTypes.values()))
                .setSaveConsumer(newValue -> {
                    if (newValue != eanConfigFile.getCloudType()){
                        eanConfigFile.setCloudType(newValue);
                        updateLayerListEntries(cloudLayerList, 1);
                    }
                })
                .build());

        // Render mode
        cloudSettings.addEntry(entryBuilder
                .startDropdownMenu(
                        new TranslatableText("Render mode"),
                        DropdownMenuBuilder.TopCellElementBuilder.of(eanConfigFile.getRenderMode(), value ->
                                switch (value) {
                                    case "NEVER_RENDER" -> CloudRenderModes.NEVER_RENDER;
                                    case "TWO_IN_ADVANCE" -> CloudRenderModes.TWO_IN_ADVANCE;
                                    case "ONE_IN_ADVANCE" -> CloudRenderModes.ONE_IN_ADVANCE;
                                    case "ALWAYS_RENDER" -> CloudRenderModes.ALWAYS_RENDER;
                                    default -> eanConfigFile.getRenderMode();
                                }),
                        DropdownMenuBuilder.CellCreatorBuilder.of(value ->
                        {
                            if (CloudRenderModes.NEVER_RENDER.equals(value)){
                                return new TranslatableText("NEVER_RENDER");
                            } else if (CloudRenderModes.TWO_IN_ADVANCE.equals(value)) {
                                return new TranslatableText("TWO_IN_ADVANCE");
                            } else if (CloudRenderModes.ONE_IN_ADVANCE.equals(value)) {
                                return new TranslatableText("ONE_IN_ADVANCE");
                            } else if (CloudRenderModes.ALWAYS_RENDER.equals(value)) {
                                return new TranslatableText("ALWAYS_RENDER");
                            }
                            return new TranslatableText("NEVER_RENDER");
                        }))
                .setDefaultValue(CloudRenderModes.ALWAYS_RENDER)
                .setTooltip(new TranslatableText("This value determines when a cloud layer begins to render."))
                .setSuggestionMode(false)
                .setSelections(Arrays.asList(CloudRenderModes.values()))
                .setSaveConsumer(newValue ->{
                        if (newValue != eanConfigFile.getRenderMode()){
                            eanConfigFile.setRenderMode(newValue);
                            updateLayerListEntries(cloudLayerList, 2);
                        }
                })
                .build());

        // LOD render mode
        cloudSettings.addEntry(entryBuilder
                .startDropdownMenu(
                        new TranslatableText("LOD render mode"),
                        DropdownMenuBuilder.TopCellElementBuilder.of(eanConfigFile.getLodRenderMode(), value ->
                                switch (value) {
                                    case "TWO_IN_ADVANCE" -> CloudRenderModes.TWO_IN_ADVANCE;
                                    case "ONE_IN_ADVANCE" -> CloudRenderModes.ONE_IN_ADVANCE;
                                    case "ALWAYS_RENDER" -> CloudRenderModes.ALWAYS_RENDER;
                                    default -> eanConfigFile.getLodRenderMode();
                                }),
                        DropdownMenuBuilder.CellCreatorBuilder.of(value ->
                        {
                            if (CloudRenderModes.TWO_IN_ADVANCE.equals(value)) {
                                return new TranslatableText("TWO_IN_ADVANCE");
                            } else if (CloudRenderModes.ONE_IN_ADVANCE.equals(value)) {
                                return new TranslatableText("ONE_IN_ADVANCE");
                            } else if (CloudRenderModes.ALWAYS_RENDER.equals(value)) {
                                return new TranslatableText("ALWAYS_RENDER");
                            }
                            return new TranslatableText("NEVER_RENDER");
                        }))
                .setDefaultValue(CloudRenderModes.TWO_IN_ADVANCE)
                .setTooltip(new TranslatableText("This value determines when a cloud layer begins to render in high level of detail. It is only used if the cloud type is set to LOD."))
                .setSuggestionMode(false)
                .setSelections(Arrays.asList(CloudRenderModes.values()))
                .setSaveConsumer(newValue ->{
                    if (newValue != eanConfigFile.getLodRenderMode()) {
                        eanConfigFile.setLodRenderMode(newValue);
                        updateLayerListEntries(cloudLayerList, 3);
                    }
                })
                .build());

        // ? Layer-specific settings
        // * Put layer attributes in a list, for each layer, to later be displayed in the config screen in order.
        int layerNum = 0;
        for(CloudLayer layer : cloudLayerList){
            // * Make list for layerNum
            layerAttributesList.add(layerNum, new ArrayList<>());

            // * Add layer attributes to layerNum list.
            // Name (Only used for display and file path)
            layer.setName("Layer " + (layerNum+1));

            // Altitude
            layerAttributesList.get(layerNum).add(entryBuilder
                    .startFloatField(new TranslatableText("Altitude"), layer.getAltitude())
                    .setDefaultValue(layer.getAltitude())
                    .setSaveConsumer(layer::setAltitude)
                    .build());

            // Cloud type
            layerAttributesList.get(layerNum).add(entryBuilder
                    .startDropdownMenu(
                            new TranslatableText("Cloud type"),
                            DropdownMenuBuilder.TopCellElementBuilder.of(layer.getCloudType(), value ->
                                    switch (value) {
                                        case "LOD" -> CloudTypes.LOD;
                                        case "FAST" -> CloudTypes.FAST;
                                        case "FANCY" -> CloudTypes.FANCY;
                                        default -> layer.getCloudType();
                                    }),
                            DropdownMenuBuilder.CellCreatorBuilder.of(value ->
                            {
                                if (CloudTypes.LOD.equals(value)) {
                                    return new TranslatableText("LOD");
                                } else if (CloudTypes.FAST.equals(value)) {
                                    return new TranslatableText("FAST");
                                } else if (CloudTypes.FANCY.equals(value)) {
                                    return new TranslatableText("FANCY");
                                }
                                return new TranslatableText("UNKNOWN CLOUD TYPE");
                            }))
                    .setDefaultValue(CloudTypes.LOD)
                    .setSuggestionMode(false)
                    .setSelections(Arrays.asList(CloudTypes.values()))
                    .setSaveConsumer(layer::setCloudType)
                    .build());

            // Render mode
            layerAttributesList.get(layerNum).add(entryBuilder
                    .startDropdownMenu(
                            new TranslatableText("Render mode"),
                            DropdownMenuBuilder.TopCellElementBuilder.of(layer.getRenderMode(), value ->
                                    switch (value) {
                                        case "NEVER_RENDER" -> CloudRenderModes.NEVER_RENDER;
                                        case "TWO_IN_ADVANCE" -> CloudRenderModes.TWO_IN_ADVANCE;
                                        case "ONE_IN_ADVANCE" -> CloudRenderModes.ONE_IN_ADVANCE;
                                        case "CUSTOM_ALTITUDE" -> CloudRenderModes.CUSTOM_ALTITUDE;
                                        case "ALWAYS_RENDER" -> CloudRenderModes.ALWAYS_RENDER;
                                        default -> layer.getRenderMode();
                                    }),
                            DropdownMenuBuilder.CellCreatorBuilder.of(value ->
                            {
                                if (CloudRenderModes.NEVER_RENDER.equals(value)){
                                    return new TranslatableText("NEVER_RENDER");
                                } else if (CloudRenderModes.TWO_IN_ADVANCE.equals(value)) {
                                    return new TranslatableText("TWO_IN_ADVANCE");
                                } else if (CloudRenderModes.ONE_IN_ADVANCE.equals(value)) {
                                    return new TranslatableText("ONE_IN_ADVANCE");
                                } else if (CloudRenderModes.CUSTOM_ALTITUDE.equals(value)) {
                                    return new TranslatableText("CUSTOM_ALTITUDE");
                                } else if (CloudRenderModes.ALWAYS_RENDER.equals(value)) {
                                    return new TranslatableText("ALWAYS_RENDER");
                                }
                                return new TranslatableText("NEVER_RENDER");
                            }))
                    .setDefaultValue(CloudRenderModes.ALWAYS_RENDER)
                    .setTooltip(new TranslatableText("This value determines when the cloud layer begins to render. It can be one/two layers in advance, at a set altitude or always render."))
                    .setSuggestionMode(false)
                    .setSelections(Arrays.asList(CloudRenderModes.values()))
                    .setSaveConsumer(layer::setRenderMode)
                    .build());

            // Render distance
            layerAttributesList.get(layerNum).add(entryBuilder
                    .startFloatField(new TranslatableText("Custom render altitude"), layer.getCloudRenderDistance())
                    .setTooltip(new TranslatableText("This value is only used if \"Custom altitude\" was selected as the render mode."))
                    .setDefaultValue(0.0F)
                    .setSaveConsumer(layer::setCloudRenderDistance)
                    .build());

            // LOD render mode
            layerAttributesList.get(layerNum).add(entryBuilder
                    .startDropdownMenu(
                            new TranslatableText("LOD render mode"),
                            DropdownMenuBuilder.TopCellElementBuilder.of(layer.getLodRenderMode(), value ->
                                    switch (value) {
                                        case "TWO_IN_ADVANCE" -> CloudRenderModes.TWO_IN_ADVANCE;
                                        case "ONE_IN_ADVANCE" -> CloudRenderModes.ONE_IN_ADVANCE;
                                        case "CUSTOM_ALTITUDE" -> CloudRenderModes.CUSTOM_ALTITUDE;
                                        case "ALWAYS_RENDER" -> CloudRenderModes.ALWAYS_RENDER;
                                        default -> layer.getLodRenderMode();
                                    }),
                            DropdownMenuBuilder.CellCreatorBuilder.of(value ->
                            {
                                if (CloudRenderModes.TWO_IN_ADVANCE.equals(value)) {
                                    return new TranslatableText("TWO_IN_ADVANCE");
                                } else if (CloudRenderModes.ONE_IN_ADVANCE.equals(value)) {
                                    return new TranslatableText("ONE_IN_ADVANCE");
                                } else if (CloudRenderModes.CUSTOM_ALTITUDE.equals(value)) {
                                    return new TranslatableText("CUSTOM_ALTITUDE");
                                } else if (CloudRenderModes.ALWAYS_RENDER.equals(value)) {
                                    return new TranslatableText("ALWAYS_RENDER");
                                }
                                return new TranslatableText("NEVER_RENDER");
                            }))
                    .setDefaultValue(CloudRenderModes.TWO_IN_ADVANCE)
                    .setTooltip(new TranslatableText("This value determines when the cloud layer begins to render in high level of detail. It is only used if the cloud type is set to LOD."))
                    .setSuggestionMode(false)
                    .setSelections(Arrays.asList(CloudRenderModes.values()))
                    .setSaveConsumer(layer::setLodRenderMode)
                    .build());

            // LOD render distance
            layerAttributesList.get(layerNum).add(entryBuilder
                    .startFloatField(new TranslatableText("Custom LOD render altitude"), layer.getLodRenderDistance())
                    .setTooltip(new TranslatableText("This value is only used if \"Custom altitude\" was selected as the LOD render mode."))
                    .setDefaultValue(0.0F)
                    .setSaveConsumer(layer::setLodRenderDistance)
                    .build());

            // Add to the counter to list next layer's attributes.
            layerNum++;
        }

        // * Create each layer subCategory with their own attribute list.
        for (int i = 0; i < layerAttributesList.size(); i++){
            layerList.add(i, entryBuilder.startSubCategory(new TranslatableText(cloudLayerList.get(i).getName()), layerAttributesList.get(i)).build());
        }

        // * Add all layer subCategories to the main config subCategory.
        cloudSettings.addEntry(entryBuilder.startTextDescription(new TranslatableText("--- Cloud layer individual configuration ---"))
                .setTooltip(new TranslatableText("If you want to set up each cloud layer individually and in most detail, here is the place!"))
                .build());

        cloudSettings.addEntry(entryBuilder.startSubCategory(new TranslatableText("Select a cloud layer:"), layerList)
                .build());
    }

    // Add empty layers if the layer amount setting is greater than the actual layer amount. Or remove layers if it is lower.
    private void updateLayerListEntries(List<CloudLayer> cloudLayerList, int parameterToChange){
        // * Add to or subtract from the cloud layer list if the layer amount value has changed.
        int layerAmount = eanConfigFile.getLayerAmount();
        int layerAmountDifference = layerAmount - cloudLayerList.size();

        // If the layer amount set is greater than the actual amount, add layers.
        if (layerAmountDifference > 0){

            // Set new layer altitude based on the last layer altitude plus the cloud distance setting.
            float lastLayerAltitude;
            if (cloudLayerList.size() > 0){
                lastLayerAltitude = cloudLayerList.get(cloudLayerList.size()-1).getAltitude();
            } else {
                lastLayerAltitude = 192.0F;
            }

            // Add a layer for each layer there's missing from the total amount.
            for(int i = 0; i<layerAmountDifference;i++){
                lastLayerAltitude += eanConfigFile.getLayerDistance();
                cloudLayerList.add(new CloudLayer(lastLayerAltitude, eanConfigFile.getCloudType(), eanConfigFile.getRenderMode(), 0.0F, eanConfigFile.getLodRenderMode(), 0.0F));
            }
        } else if (layerAmountDifference < 0){
            while (cloudLayerList.size()>layerAmount) {
                cloudLayerList.remove(cloudLayerList.size()-1);
            }
        }

        System.out.println("Updated layers, current layer amount = " + cloudLayerList.size());

        // * Change a parameter of every cloud layer if it has been indicated.
        List<CloudLayer> auxList = new ArrayList<>();
        switch (parameterToChange) {
            case 0 -> {
                float altitude = eanConfigFile.getStackingAltitude();
                for (CloudLayer layer : cloudLayerList) {
                    altitude += eanConfigFile.getLayerDistance();
                    auxList.add(new CloudLayer(altitude, layer.getCloudType(), layer.getRenderMode(), layer.getCloudRenderDistance(), layer.getLodRenderMode(), layer.getLodRenderDistance()));
                }
                cloudLayerList.clear();
                cloudLayerList.addAll(auxList);
            }
            case 1 -> {
                for (CloudLayer layer : cloudLayerList) {
                    auxList.add(new CloudLayer(layer.getAltitude(), eanConfigFile.getCloudType(), layer.getRenderMode(), layer.getCloudRenderDistance(), layer.getLodRenderMode(), layer.getLodRenderDistance()));
                }
                cloudLayerList.clear();
                cloudLayerList.addAll(auxList);
            }
            case 2 -> {
                for (CloudLayer layer : cloudLayerList) {
                    auxList.add(new CloudLayer(layer.getAltitude(), layer.getCloudType(), eanConfigFile.getRenderMode(), layer.getCloudRenderDistance(), layer.getLodRenderMode(), layer.getLodRenderDistance()));
                }
                cloudLayerList.clear();
                cloudLayerList.addAll(auxList);
            }
            case 3 -> {
                for (CloudLayer layer : cloudLayerList) {
                    auxList.add(new CloudLayer(layer.getAltitude(), layer.getCloudType(), layer.getRenderMode(), layer.getCloudRenderDistance(), eanConfigFile.getLodRenderMode(), layer.getLodRenderDistance()));
                }
                cloudLayerList.clear();
                cloudLayerList.addAll(auxList);
            }
            default -> {
            }
        }
    }
}
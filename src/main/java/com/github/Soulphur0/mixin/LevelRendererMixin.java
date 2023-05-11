package com.github.Soulphur0.mixin;

import com.github.Soulphur0.ElytraAeronautics;
import com.github.Soulphur0.config.*;
import com.github.Soulphur0.utility.EanMath;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;


@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin implements ResourceManagerReloadListener, AutoCloseable {

    // * Class attributes used by the renderClouds() public method.
    @Shadow
    private ClientLevel level;

    @Shadow
    private int prevCloudX;

    @Shadow
    private int prevCloudY;

    @Shadow
    private int prevCloudZ;

    @Shadow
    private Vec3 prevCloudColor;

    @Shadow
    @Nullable
    private CloudStatus prevCloudsType;

    @Shadow
    private int ticks;

    @Final
    @Shadow
    private Minecraft minecraft;

    @Shadow
    private boolean generateClouds;

    @Final
    @Shadow
    private static ResourceLocation CLOUDS_LOCATION;

    @Shadow
    private VertexBuffer cloudBuffer;

    @Inject(method = "renderClouds", at = @At(value = "HEAD"), cancellable = true)
    private void renderCloudsPublicOverwrite(PoseStack matrices, Matrix4f projectionMatrix, float tickDelta, double d, double e, double f, CallbackInfo ci) {
        if (level.effects().renderClouds(level, ticks, tickDelta, matrices, d, e, f, projectionMatrix)) {
            ci.cancel();
        }
        float g = this.level.effects().getCloudHeight();
        if (!Float.isNaN(g)) {
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.depthMask(true);
            float h = 12.0F;
            float i = 4.0F;
            double j = 2.0E-4D;
            double k = (double) (((float) this.ticks + tickDelta) * 0.03F);
            double l = (d + k) / 12.0D;
            double m = (double) (g - (float) e + 0.33F);
            double n = f / 12.0D + 0.33000001311302185D;
            l -= (double) (Mth.floor(l / 2048.0D) * 2048);
            n -= (double) (Mth.floor(n / 2048.0D) * 2048);
            float o = (float) (l - (double) Mth.floor(l));
            float p = (float) (m / 4.0D - (double) Mth.floor(m / 4.0D)) * 4.0F;
            float q = (float) (n - (double) Mth.floor(n));
            Vec3 vec3d = this.level.getCloudColor(tickDelta);
            int r = (int) Math.floor(l);
            int s = (int) Math.floor(m / 4.0D);
            int t = (int) Math.floor(n);
            if (r != this.prevCloudX || s != this.prevCloudY || t != this.prevCloudZ || this.minecraft.options.getCloudsType() != this.prevCloudsType || this.prevCloudColor.distanceToSqr(vec3d) > 2.0E-4D) {
                this.prevCloudX = r;
                this.prevCloudY = s;
                this.prevCloudZ = t;
                this.prevCloudColor = vec3d;
                this.prevCloudsType = this.minecraft.options.getCloudsType();
                this.generateClouds = true;
            }

            if (this.generateClouds) {
                this.generateClouds = false;
                BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
                if (this.cloudBuffer != null) {
                    this.cloudBuffer.close();
                }

                this.cloudBuffer = new VertexBuffer();
                BufferBuilder.RenderedBuffer builtBuffer = eanCustomCloudRenderSetup(bufferBuilder, l, m, n, vec3d);
                this.cloudBuffer.bind();
                this.cloudBuffer.upload(builtBuffer);
                VertexBuffer.unbind();
            }

            RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
            RenderSystem.setShaderTexture(0, CLOUDS_LOCATION);
            FogRenderer.setupNoFog();
            matrices.pushPose();
            matrices.scale(12.0F, 1.0F, 12.0F);
            matrices.translate((double) (-o), (double) p, (double) (-q));
            if (this.cloudBuffer != null) {
                this.cloudBuffer.bind();
                int u = this.prevCloudsType == CloudStatus.FANCY ? 0 : 1;

                for (int v = u; v < 2; ++v) {
                    if (v == 0) {
                        RenderSystem.colorMask(false, false, false, false);
                    } else {
                        RenderSystem.colorMask(true, true, true, true);
                    }

                    ShaderInstance shaderProgram = RenderSystem.getShader();
                    this.cloudBuffer.drawWithShader(matrices.last().pose(), projectionMatrix, shaderProgram);
                }

                VertexBuffer.unbind();
            }

            matrices.popPose();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    EanConfigFile configFile;
    List<CloudLayer> cloudLayers = new ArrayList<>();

    private BufferBuilder.RenderedBuffer eanCustomCloudRenderSetup(BufferBuilder builder, double x, double renderCloudsY, double z, Vec3 color) {
        // ? Get cloud layers from config file (once per save action)
        if (ElytraAeronautics.readConfigFileCue_WorldRendererMixin) {
            configFile = ConfigFileReader.getConfigFile();
            cloudLayers = configFile.getCloudLayerList();
            ElytraAeronautics.readConfigFileCue_WorldRendererMixin = false;
        }

        // ? Return buffer with all cloud layers drawn as the config specifies.
        return renderCloudsPrivateOverwrite(cloudLayers, builder, x, renderCloudsY, z, color);
    }

    // FIXME There are two current issues regarding this method:
    //  Cloud configuration is not working as intended.
    //  The bottom face of clouds doesn't render when looking at it from the lower half of the cloud inside the cloud.
    //  When using smooth LODs, clouds sometimes pop-in at full size before returning flat and starting to puff up.
    private BufferBuilder.RenderedBuffer renderCloudsPrivateOverwrite(List<CloudLayer> cloudLayers, BufferBuilder builder, double x, double renderCloudsY, double z, Vec3 color) {
        float f = 4.0F;
        float g = 0.00390625F;
        float h = 9.765625E-4F;
        float k = (float) Mth.floor(x) * 0.00390625F;
        float l = (float) Mth.floor(z) * 0.00390625F;
        float m = (float) color.x;
        float n = (float) color.y;
        float o = (float) color.z;
        float p = m * 0.9F;
        float q = n * 0.9F;
        float r = o * 0.9F;
        float s = m * 0.7F;
        float t = n * 0.7F;
        float u = o * 0.7F;
        float v = m * 0.8F;
        float w = n * 0.8F;
        float aa = o * 0.8F;
        RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
        float playerRelativeDistanceFromCloudLayer = (float)Math.floor(renderCloudsY / 4.0D) * 4.0F; // Unmodified original variable.

        // $ ↓↓↓↓↓ BIG CUSTOM CODE BLOCK ↓↓↓↓↓

        // ? Used to displace each layer by 1*constant horizontally, and to calculate relative positions between layers.
        int layerCounter = 0;

        // ? Used to keep track of previous layer's altitudes when placing cloud layers.
        float prePreAltitude = 0;
        float distFromPrePreLayer;

        float preAltitude = 0;
        float distFromPreLayer;

        // ? Used to determine the distance to a non-existing previous layer, or to a layer that always renders.
        float infinity = Float.MAX_VALUE;

        // *** Process each layer ***
        for (CloudLayer layer : cloudLayers){
            // _ Get layer attributes.
            // ? Displacement variables.
            float horizontalDisplacement = (layerCounter + 1)*100; // How displaced are clouds horizontally from the default cloud layer, (this avoids both layers to have the same cloud pattern)
            float verticalDisplacement = layer.getAltitude()-192.0F; // Additional altitude from the default cloud height.

            // ? Cloud render variables.
            CloudTypes cloudType = layer.getCloudType();
            CloudRenderModes renderMode = layer.getRenderMode();
            CloudRenderModes lodRenderMode = layer.getLodRenderMode();
            boolean usingSmoothLODs = layer.isUseSmoothLODs();

            // ? Cloud render distance variables.
            float renderDistance = layer.getCloudRenderDistance();
            boolean usingCustomRenderDistance = renderMode == CloudRenderModes.CUSTOM_ALTITUDE;

            float lodRenderDistance = layer.getLodRenderDistance();
            boolean usingCustomLODRenderDistance = lodRenderMode == CloudRenderModes.CUSTOM_ALTITUDE;

            // _ Calculate render distance.
            // - Calculate distance from two layers behind.
            if (layerCounter == 0)
                distFromPrePreLayer = infinity; // Infinity, the distance of the first layer from its previous one.
            else if (layerCounter == 1)
                distFromPrePreLayer = layer.getAltitude() - 192.0F;
            else
                distFromPrePreLayer = layer.getAltitude() - prePreAltitude;

            // - Calculate distance from the layer behind.
            distFromPreLayer = (layerCounter > 0) ? layer.getAltitude() - preAltitude/2 : layer.getAltitude();

            // - Update previous altitudes, move altitudes one position behind.
            prePreAltitude = preAltitude;
            preAltitude = layer.getAltitude();

            // - Now that numbers are calculated for each mode, set which relative distance mode to use.
            switch (renderMode){
                case TWO_IN_ADVANCE -> renderDistance = distFromPreLayer; // TODO: These are temporarily swapped
                case ONE_IN_ADVANCE -> renderDistance = distFromPrePreLayer; // TODO: These are temporarily swapped
                case ALWAYS_RENDER -> renderDistance = infinity; // Infinity, the render distance to a layer that always renders.
                default -> {}
            }

            switch (lodRenderMode){
                case TWO_IN_ADVANCE -> lodRenderDistance = distFromPrePreLayer;
                case ONE_IN_ADVANCE -> lodRenderDistance = distFromPreLayer;
                case ALWAYS_RENDER -> lodRenderDistance = infinity; // Infinity, the render distance to a layer that always renders.
                default -> {}
            }

            // - Skip render layer if render mode is 'NEVER_RENDER'
            if (CloudRenderModes.NEVER_RENDER.equals(renderMode)) continue;

            // _ Use renderDistance to determine how to render clouds relative to the player.
            // - Check if the player is within the render distance either if custom render altitude is being used or not.
            playerRelativeDistanceFromCloudLayer += verticalDisplacement; // Player distance-to the cloud layer.
            float cloudAltitude = layer.getAltitude(); // Absolute height position of the cloud layer.

            boolean withinRenderDistance = (usingCustomRenderDistance) ? playerRelativeDistanceFromCloudLayer < (cloudAltitude-renderDistance): playerRelativeDistanceFromCloudLayer < renderDistance;
            boolean withinHighLODDistance = (usingCustomLODRenderDistance) ? playerRelativeDistanceFromCloudLayer < (cloudAltitude-lodRenderDistance): playerRelativeDistanceFromCloudLayer < lodRenderDistance;

            // - Puff up clouds.
            float cloudThickness = 4.0F;
            if (usingSmoothLODs) {
                // ? Sets puff-up start and puff-up stop distances.
                float puffUpStartDistance = (lodRenderDistance+4.0F)/2;
                float puffUpStopDistance = (lodRenderDistance+4.0F)/5;

                // ? If the player is too far away, clouds appear as fast clouds, else they linearly puff-up.
                if (CloudTypes.LOD.equals(cloudType) && playerRelativeDistanceFromCloudLayer > puffUpStartDistance){
                    cloudThickness = 0.0F;
                } else if (CloudTypes.LOD.equals(cloudType) && playerRelativeDistanceFromCloudLayer < puffUpStartDistance && playerRelativeDistanceFromCloudLayer > puffUpStopDistance){
                    cloudThickness = EanMath.getLinealValue(puffUpStartDistance,0,puffUpStopDistance,4,playerRelativeDistanceFromCloudLayer);
                }
            }

            // $ ↑↑↑↑↑ BIG CUSTOM CODE BLOCK ↑↑↑↑↑

            // _ Render layers
            // * RENDER FANCY clouds either if (fancy clouds are enabled and withing render range) or (within high LOD altitude range and maximum LOD render distance).
            if (cloudType.equals(CloudTypes.FANCY) && withinRenderDistance || cloudType.equals(CloudTypes.LOD) && withinHighLODDistance) {
                for (int ac = Mth.floor(-0.125 * horizontalDisplacement - 3); ac <= Mth.floor(-0.125 * horizontalDisplacement + 4); ++ac) {
                    for (int ad = -3; ad <= 4; ++ad) {
                        float ae = (float) (ac * 8);
                        float af = (float) (ad * 8);

                        // This renders the bottom face of clouds.
                        if (playerRelativeDistanceFromCloudLayer > -6.0F) {
                            builder.vertex(ae + 0.0F + horizontalDisplacement, playerRelativeDistanceFromCloudLayer + 0.0F, af + 8.0F).uv((ae + 0.0F) * 0.00390625F + k, (af + 8.0F) * 0.00390625F + l).color(s, t, u, 0.8F).normal(0.0F, -1.0F, 0.0F).endVertex();
                            builder.vertex(ae + 8.0F + horizontalDisplacement, playerRelativeDistanceFromCloudLayer + 0.0F, af + 8.0F).uv((ae + 8.0F) * 0.00390625F + k, (af + 8.0F) * 0.00390625F + l).color(s, t, u, 0.8F).normal(0.0F, -1.0F, 0.0F).endVertex();
                            builder.vertex(ae + 8.0F + horizontalDisplacement, playerRelativeDistanceFromCloudLayer + 0.0F, af + 0.0F).uv((ae + 8.0F) * 0.00390625F + k, (af + 0.0F) * 0.00390625F + l).color(s, t, u, 0.8F).normal(0.0F, -1.0F, 0.0F).endVertex();
                            builder.vertex(ae + 0.0F + horizontalDisplacement, playerRelativeDistanceFromCloudLayer + 0.0F, af + 0.0F).uv((ae + 0.0F) * 0.00390625F + k, (af + 0.0F) * 0.00390625F + l).color(s, t, u, 0.8F).normal(0.0F, -1.0F, 0.0F).endVertex();
                        }

                        // This renders the top face of clouds.
                        if (playerRelativeDistanceFromCloudLayer <= 5.0F) {
                            builder.vertex(ae + 0.0F + horizontalDisplacement, playerRelativeDistanceFromCloudLayer + cloudThickness - 9.765625E-4F, af + 8.0F).uv((ae + 0.0F) * 0.00390625F + k, (af + 8.0F) * 0.00390625F + l).color(m, n, o, 0.8F).normal(0.0F, 1.0F, 0.0F).endVertex();
                            builder.vertex(ae + 8.0F + horizontalDisplacement, playerRelativeDistanceFromCloudLayer + cloudThickness - 9.765625E-4F, af + 8.0F).uv((ae + 8.0F) * 0.00390625F + k, (af + 8.0F) * 0.00390625F + l).color(m, n, o, 0.8F).normal(0.0F, 1.0F, 0.0F).endVertex();
                            builder.vertex(ae + 8.0F + horizontalDisplacement, playerRelativeDistanceFromCloudLayer + cloudThickness - 9.765625E-4F, af + 0.0F).uv((ae + 8.0F) * 0.00390625F + k, (af + 0.0F) * 0.00390625F + l).color(m, n, o, 0.8F).normal(0.0F, 1.0F, 0.0F).endVertex();
                            builder.vertex(ae + 0.0F + horizontalDisplacement, playerRelativeDistanceFromCloudLayer + cloudThickness - 9.765625E-4F, af + 0.0F).uv((ae + 0.0F) * 0.00390625F + k, (af + 0.0F) * 0.00390625F + l).color(m, n, o, 0.8F).normal(0.0F, 1.0F, 0.0F).endVertex();
                        }

                        int aj;
                        // This renders the left face of clouds.
                        // Horizontal displacement is added to the if statement to properly cull the west face of clouds.
                        if (ac > -1 - horizontalDisplacement) {
                            for(aj = 0; aj < 8; ++aj) {
                                builder.vertex(ae + (float) aj + 0.0F + horizontalDisplacement, playerRelativeDistanceFromCloudLayer + 0.0F, af + 8.0F).uv((ae + (float) aj + 0.5F) * 0.00390625F + k, (af + 8.0F) * 0.00390625F + l).color(p, q, r, 0.8F).normal(-1.0F, 0.0F, 0.0F).endVertex();
                                builder.vertex(ae + (float) aj + 0.0F + horizontalDisplacement, playerRelativeDistanceFromCloudLayer + cloudThickness, af + 8.0F).uv((ae + (float) aj + 0.5F) * 0.00390625F + k, (af + 8.0F) * 0.00390625F + l).color(p, q, r, 0.8F).normal(-1.0F, 0.0F, 0.0F).endVertex();
                                builder.vertex(ae + (float) aj + 0.0F + horizontalDisplacement, playerRelativeDistanceFromCloudLayer + cloudThickness, af + 0.0F).uv((ae + (float) aj + 0.5F) * 0.00390625F + k, (af + 0.0F) * 0.00390625F + l).color(p, q, r, 0.8F).normal(-1.0F, 0.0F, 0.0F).endVertex();
                                builder.vertex(ae + (float) aj + 0.0F + horizontalDisplacement, playerRelativeDistanceFromCloudLayer + 0.0F, af + 0.0F).uv((ae + (float) aj + 0.5F) * 0.00390625F + k, (af + 0.0F) * 0.00390625F + l).color(p, q, r, 0.8F).normal(-1.0F, 0.0F, 0.0F).endVertex();
                            }
                        }

                        if (ac <= 1) {
                            // This renders the right face of clouds.
                            for(aj = 0; aj < 8; ++aj) {
                                builder.vertex(ae + (float) aj + 1.0F - 9.765625E-4F + horizontalDisplacement, playerRelativeDistanceFromCloudLayer + 0.0F, af + 8.0F).uv((ae + (float) aj + 0.5F) * 0.00390625F + k, (af + 8.0F) * 0.00390625F + l).color(p, q, r, 0.8F).normal(1.0F, 0.0F, 0.0F).endVertex();
                                builder.vertex(ae + (float) aj + 1.0F - 9.765625E-4F + horizontalDisplacement, playerRelativeDistanceFromCloudLayer + cloudThickness, af + 8.0F).uv((ae + (float) aj + 0.5F) * 0.00390625F + k, (af + 8.0F) * 0.00390625F + l).color(p, q, r, 0.8F).normal(1.0F, 0.0F, 0.0F).endVertex();
                                builder.vertex(ae + (float) aj + 1.0F - 9.765625E-4F + horizontalDisplacement, playerRelativeDistanceFromCloudLayer + cloudThickness, af + 0.0F).uv((ae + (float) aj + 0.5F) * 0.00390625F + k, (af + 0.0F) * 0.00390625F + l).color(p, q, r, 0.8F).normal(1.0F, 0.0F, 0.0F).endVertex();
                                builder.vertex(ae + (float) aj + 1.0F - 9.765625E-4F + horizontalDisplacement, playerRelativeDistanceFromCloudLayer + 0.0F, af + 0.0F).uv((ae + (float) aj + 0.5F) * 0.00390625F + k, (af + 0.0F) * 0.00390625F + l).color(p, q, r, 0.8F).normal(1.0F, 0.0F, 0.0F).endVertex();
                            }
                        }
                        // This renders the front(north) face of clouds.
                        if (ad > -1) {
                            for(aj = 0; aj < 8; ++aj) {
                                builder.vertex(ae + 0.0F + horizontalDisplacement, playerRelativeDistanceFromCloudLayer + cloudThickness, af + (float) aj + 0.0F).uv((ae + 0.0F) * 0.00390625F + k, (af + (float) aj + 0.5F) * 0.00390625F + l).color(v, w, aa, 0.8F).normal(0.0F, 0.0F, -1.0F).endVertex();
                                builder.vertex(ae + 8.0F + horizontalDisplacement, playerRelativeDistanceFromCloudLayer + cloudThickness, af + (float) aj + 0.0F).uv((ae + 8.0F) * 0.00390625F + k, (af + (float) aj + 0.5F) * 0.00390625F + l).color(v, w, aa, 0.8F).normal(0.0F, 0.0F, -1.0F).endVertex();
                                builder.vertex(ae + 8.0F + horizontalDisplacement, playerRelativeDistanceFromCloudLayer + 0.0F, af + (float) aj + 0.0F).uv((ae + 8.0F) * 0.00390625F + k, (af + (float) aj + 0.5F) * 0.00390625F + l).color(v, w, aa, 0.8F).normal(0.0F, 0.0F, -1.0F).endVertex();
                                builder.vertex(ae + 0.0F + horizontalDisplacement, playerRelativeDistanceFromCloudLayer + 0.0F, af + (float) aj + 0.0F).uv((ae + 0.0F) * 0.00390625F + k, (af + (float) aj + 0.5F) * 0.00390625F + l).color(v, w, aa, 0.8F).normal(0.0F, 0.0F, -1.0F).endVertex();
                            }
                        }
                        // This renders the back(south) face of clouds.
                        if (ad <= 1) {
                            for(aj = 0; aj < 8; ++aj) {
                                builder.vertex(ae + 0.0F + horizontalDisplacement, playerRelativeDistanceFromCloudLayer + cloudThickness, af + (float) aj + 1.0F - 9.765625E-4F).uv((ae + 0.0F) * 0.00390625F + k, (af + (float) aj + 0.5F) * 0.00390625F + l).color(v, w, aa, 0.8F).normal(0.0F, 0.0F, 1.0F).endVertex();
                                builder.vertex(ae + 8.0F + horizontalDisplacement, playerRelativeDistanceFromCloudLayer + cloudThickness, af + (float) aj + 1.0F - 9.765625E-4F).uv((ae + 8.0F) * 0.00390625F + k, (af + (float) aj + 0.5F) * 0.00390625F + l).color(v, w, aa, 0.8F).normal(0.0F, 0.0F, 1.0F).endVertex();
                                builder.vertex(ae + 8.0F + horizontalDisplacement, playerRelativeDistanceFromCloudLayer + 0.0F, af + (float) aj + 1.0F - 9.765625E-4F).uv((ae + 8.0F) * 0.00390625F + k, (af + (float) aj + 0.5F) * 0.00390625F + l).color(v, w, aa, 0.8F).normal(0.0F, 0.0F, 1.0F).endVertex();
                                builder.vertex(ae + 0.0F + horizontalDisplacement, playerRelativeDistanceFromCloudLayer + 0.0F, af + (float) aj + 1.0F - 9.765625E-4F).uv((ae + 0.0F) * 0.00390625F + k, (af + (float) aj + 0.5F) * 0.00390625F + l).color(v, w, aa, 0.8F).normal(0.0F, 0.0F, 1.0F).endVertex();
                            }
                        }
                    }
                }
            }
            // * RENDER FAST clouds either if (fast clouds are enabled and withing render range) or (within maximum LOD render distance).
            else if (cloudType.equals(CloudTypes.FAST) && withinRenderDistance || cloudType.equals(CloudTypes.LOD) && withinRenderDistance) {
                for (int ac = Mth.floor(-0.125 * horizontalDisplacement - 3); ac <= Mth.floor(-0.125 * horizontalDisplacement + 4); ++ac) {
                    for (int ad = -3; ad <= 4; ++ad) {
                        float ae = (float) (ac * 8);
                        float af = (float) (ad * 8);
                        builder.vertex(ae + 0.0F + horizontalDisplacement, playerRelativeDistanceFromCloudLayer + 0.0F, af + 8.0F).uv((ae + 0.0F) * 0.00390625F + k, (af + 8.0F) * 0.00390625F + l).color(m, n, o, 0.8F).normal(0.0F, -1.0F, 0.0F).endVertex();
                        builder.vertex(ae + 8.0F + horizontalDisplacement, playerRelativeDistanceFromCloudLayer + 0.0F, af + 8.0F).uv((ae + 8.0F) * 0.00390625F + k, (af + 8.0F) * 0.00390625F + l).color(m, n, o, 0.8F).normal(0.0F, -1.0F, 0.0F).endVertex();
                        builder.vertex(ae + 8.0F + horizontalDisplacement, playerRelativeDistanceFromCloudLayer + 0.0F, af + 0.0F).uv((ae + 8.0F) * 0.00390625F + k, (af + 0.0F) * 0.00390625F + l).color(m, n, o, 0.8F).normal(0.0F, -1.0F, 0.0F).endVertex();
                        builder.vertex(ae + 0.0F + horizontalDisplacement, playerRelativeDistanceFromCloudLayer + 0.0F, af + 0.0F).uv((ae + 0.0F) * 0.00390625F + k, (af + 0.0F) * 0.00390625F + l).color(m, n, o, 0.8F).normal(0.0F, -1.0F, 0.0F).endVertex();
                    }
                }
            }

            layerCounter++;
        }

        return builder.end();
    }
}
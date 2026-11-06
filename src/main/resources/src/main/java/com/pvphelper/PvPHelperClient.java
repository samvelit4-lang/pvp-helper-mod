package com.pvphelper;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;

public class PvPHelperClient implements ClientModInitializer {
    private static KeyBinding crystalAuraKey;
    private static KeyBinding pvpAiKey;
    private static KeyBinding guiToggleKey;

    private static boolean isCrystalAuraEnabled = false;
    private static boolean isPvpAiEnabled = false;
    private static boolean isGuiOpen = false;
    
    private static final double REACH_DISTANCE = 4.5;

    @Override
    public void onInitializeClient() {
        crystalAuraKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pvphelper.crystalaura", InputUtil.Type.KEY_SYM, GLFW.GLFW_KEY_C, "category.pvphelper.general"
        ));
        pvpAiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pvphelper.pvpai", InputUtil.Type.KEY_SYM, GLFW.GLFW_KEY_K, "category.pvphelper.general"
        ));
        guiToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pvphelper.gui", InputUtil.Type.KEY_SYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.pvphelper.general"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            while (crystalAuraKey.wasPressed()) {
                isCrystalAuraEnabled = !isCrystalAuraEnabled;
                client.player.sendMessage(Text.literal("§8[§cFunTime§8] §7Crystal Aura: " + (isCrystalAuraEnabled ? "§aВКЛ" : "§cВЫКЛ")), true);
            }
            while (pvpAiKey.wasPressed()) {
                isPvpAiEnabled = !isPvpAiEnabled;
                client.player.sendMessage(Text.literal("§8[§cFunTime§8] §7PvP AI Target: " + (isPvpAiEnabled ? "§aВКЛ" : "§cВЫКЛ")), true);
            }
            while (guiToggleKey.wasPressed()) {
                isGuiOpen = !isGuiOpen;
            }

            if (isCrystalAuraEnabled) runCrystalAura(client);
            if (isPvpAiEnabled) runPvpAi(client);
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;
            drawNursultanStyleHud(drawContext, client);
        });
    }

    private void runCrystalAura(MinecraftClient client) {
        EndCrystalEntity crystal = client.world.getEntitiesByClass(
                EndCrystalEntity.class, client.player.getBoundingBox().expand(REACH_DISTANCE),
                c -> c.isAlive() && client.player.distanceTo(c) <= REACH_DISTANCE
        ).stream().min(Comparator.comparingDouble(c -> client.player.distanceTo(c))).orElse(null);

        if (crystal != null && client.interactionManager != null) {
            lookAtEntity(client, crystal);
            client.interactionManager.attackEntity(client.player, crystal);
            client.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private void runPvpAi(MinecraftClient client) {
        PlayerEntity target = client.world.getEntitiesByClass(
                PlayerEntity.class, client.player.getBoundingBox().expand(REACH_DISTANCE),
                p -> p != client.player && p.isAlive() && client.player.distanceTo(p) <= REACH_DISTANCE
        ).stream().min(Comparator.comparingDouble(p -> client.player.distanceTo(p))).orElse(null);

        if (target != null) lookAtEntity(client, target);
    }

    private void lookAtEntity(MinecraftClient client, net.minecraft.entity.Entity entity) {
        Vec3d eyesPos = client.player.getEyePos();
        Vec3d targetPos = entity.getEyePos();
        double diffX = targetPos.x - eyesPos.x;
        double diffY = targetPos.y - eyesPos.y;
        double diffZ = targetPos.z - eyesPos.z;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));

        client.player.setYaw(client.player.getYaw() + MathHelper.wrapDegrees(yaw - client.player.getYaw()));
        client.player.setPitch(client.player.getPitch() + MathHelper.wrapDegrees(pitch - client.player.getPitch()));
    }

    private void drawNursultanStyleHud(DrawContext context, MinecraftClient client) {
        int x = 10;
        int y = 10;
        
        context.fill(x, y, x + 140, y + 45, 0xC00F0F13);
        context.fill(x, y, x + 140, y + 2, 0xFFFF3355);

        context.drawText(client.textRenderer, "§lFunTime PvP HUD", x + 6, y + 7, 0xFFFFFFFF, true);
        context.drawText(client.textRenderer, "Crystal Aura: " + (isCrystalAuraEnabled ? "§aON" : "§cOFF"), x + 6, y + 19, 0xFFBBBBBB, true);
        context.drawText(client.textRenderer, "PvP AI: " + (isPvpAiEnabled ? "§aON" : "§cOFF"), x + 6, y + 30, 0xFFBBBBBB, true);

        if (isGuiOpen) {
            int screenWidth = client.getWindow().getScaledWidth();
            int screenHeight = client.getWindow().getScaledHeight();
            
            int guiX = screenWidth / 2 - 120;
            int guiY = screenHeight / 2 - 80;
            
            context.fill(guiX, guiY, guiX + 240, guiY + 160, 0xEE121218);
            context.fill(guiX, guiY, guiX + 240, guiY + 18, 0xFF1F1F2B);
            context.drawBorder(guiX, guiY, 240, 160, 0xFFFF3355);
            
            context.drawText(client.textRenderer, "§lNursultan Style Menu (FunTime)", guiX + 8, guiY + 5, 0xFFFFFFFF, true);
            context.drawText(client.textRenderer, "§7[C] Crystal Aura", guiX + 12, guiY + 30, 0xFFAAAAAA, true);
            context.drawText(client.textRenderer, "§7[K] PvP AI Lock", guiX + 12, guiY + 45, 0xFFAAAAAA, true);
            context.drawText(client.textRenderer, "§eНажми Right Shift для закрытия", guiX + 12, guiY + 140, 0xFFFFCC00, true);
        }
    }
}

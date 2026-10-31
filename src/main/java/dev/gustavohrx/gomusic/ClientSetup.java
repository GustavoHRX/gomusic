package dev.gustavohrx.gomusic;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

final class ClientSetup {

    private static MusicController controller;

    private ClientSetup() {
    }

    static void init() {
        MusicFolder.prepare();
        controller = new MusicController();
        controller.reload();

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(MusicFolder::onAddPackFinders);

        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        forgeBus.addListener(ClientSetup::onClientTick);
        forgeBus.addListener(ClientSetup::onPlaySound);
        forgeBus.addListener(ClientSetup::onBossBar);
        forgeBus.addListener(ClientSetup::onRegisterCommands);
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            controller.tick(Minecraft.getInstance());
        }
    }

    private static void onPlaySound(PlaySoundEvent event) {
        var sound = event.getSound();
        if (sound == null || sound instanceof TrackSound) {
            return;
        }
        if (sound.getSource() == SoundSource.RECORDS) {
            controller.onRecordStarted(sound);
        } else if (sound.getSource() == SoundSource.MUSIC && controller.hasRules()) {
            // Mutes the game's music; jukebox records use RECORDS and keep playing.
            event.setSound(null);
        }
    }

    private static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        controller.situation().onBossBarDrawn();
    }

    private static void onRegisterCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(GoMusic.MOD_ID)
                .then(Commands.literal("status").executes(ClientSetup::status))
                .then(Commands.literal("reload").executes(ClientSetup::reload))
                .then(Commands.literal("skip").executes(ClientSetup::skip))
                .then(Commands.literal("reset").executes(ClientSetup::reset)));
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        for (String line : controller.describe()) {
            context.getSource().sendSystemMessage(Component.literal(line));
        }
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        controller.reload();
        context.getSource().sendSystemMessage(Component.literal(
                "[GoMusic] " + controller.ruleCount() + " rules loaded."));
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> context) {
        controller.reset(Minecraft.getInstance());
        context.getSource().sendSystemMessage(Component.literal(
                "[GoMusic] Sound system restarted. Music will resume based on the current situation."));
        return 1;
    }

    private static int skip(CommandContext<CommandSourceStack> context) {
        controller.skip();
        return 1;
    }
}

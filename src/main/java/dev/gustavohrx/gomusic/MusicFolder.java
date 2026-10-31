package dev.gustavohrx.gomusic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The game's "gomusic" folder: rules.json plus an always-active resource pack with the .ogg files.
 * Minecraft's sound system only reads files from packs, so the folder is registered as one.
 */
final class MusicFolder {

    static final Path ROOT = FMLPaths.GAMEDIR.get().resolve(GoMusic.MOD_ID);
    static final Path RULES = ROOT.resolve("rules.json");
    static final Path HOMES = ROOT.resolve("homes.json");
    static final Path SOUNDS = ROOT.resolve("assets").resolve(GoMusic.MOD_ID).resolve("sounds");

    private static final String PACK_MCMETA = """
            {
              "pack": {
                "description": "GoMusic tracks",
                "pack_format": 15
              }
            }
            """;

    private static final String RULES_EXAMPLE = """
            {
              // Copy this file as rules.json to activate the mod.
              // The first valid rule wins. Tracks go in assets/gomusic/sounds; list them without the .ogg extension.
              "fadeSeconds": 3,
              "silenceSeconds": 20,
              "switchDelaySeconds": 2,
              "rules": [
                { "when": ["BOSS"], "tracks": ["boss"], "holdSeconds": 5 },
                { "when": ["MOBS=4"], "tracks": ["combat"], "holdSeconds": 15 },
                { "when": ["DIM=nether"], "tracks": ["nether"] },
                { "when": ["NIGHT", "RAIN||STORM"], "tracks": ["rainy_night"] },
                { "when": ["MAIN_MENU"], "tracks": ["menu"] },
                { "when": ["DAY", "!UNDERGROUND"], "tracks": ["day_1", "day_2"], "interrupt": false }
              ]
            }
            """;

    private MusicFolder() {
    }

    static void prepare() {
        try {
            Files.createDirectories(SOUNDS);
            writeIfMissing(ROOT.resolve("pack.mcmeta"), PACK_MCMETA);
            writeIfMissing(ROOT.resolve("rules.example.json"), RULES_EXAMPLE);
        } catch (IOException e) {
            GoMusic.LOGGER.error("[GoMusic] Could not prepare the folder {}", ROOT, e);
        }
    }

    private static void writeIfMissing(Path file, String content) throws IOException {
        if (Files.notExists(file)) {
            Files.writeString(file, content, StandardCharsets.UTF_8);
        }
    }

    static boolean trackExists(String track) {
        return Files.isRegularFile(SOUNDS.resolve(track + ".ogg"));
    }

    static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }
        event.addRepositorySource(consumer -> {
            Pack pack = Pack.readMetaAndCreate(
                    GoMusic.MOD_ID + "_tracks",
                    Component.literal("GoMusic"),
                    true,
                    id -> new PathPackResources(id, ROOT, false),
                    PackType.CLIENT_RESOURCES,
                    Pack.Position.TOP,
                    PackSource.BUILT_IN);
            if (pack != null) {
                consumer.accept(pack);
            } else {
                GoMusic.LOGGER.error("[GoMusic] Invalid pack at {}", ROOT);
            }
        });
    }
}

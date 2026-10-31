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
 * A pasta "gomusic" do jogo: rules.json + um pacote de recursos sempre ativo com os .ogg.
 * O sistema de som do Minecraft só lê arquivos de pacotes, por isso a pasta é registrada como um.
 */
final class MusicFolder {

    static final Path ROOT = FMLPaths.GAMEDIR.get().resolve(GoMusic.MOD_ID);
    static final Path RULES = ROOT.resolve("rules.json");
    static final Path HOMES = ROOT.resolve("homes.json");
    static final Path SOUNDS = ROOT.resolve("assets").resolve(GoMusic.MOD_ID).resolve("sounds");

    private static final String PACK_MCMETA = """
            {
              "pack": {
                "description": "GoMusic - faixas do modpack",
                "pack_format": 15
              }
            }
            """;

    private static final String RULES_EXAMPLE = """
            {
              // Copie este arquivo como rules.json para ativar o mod.
              // A primeira regra válida vence. As faixas ficam em assets/gomusic/sounds, sem a extensão .ogg.
              "fadeSeconds": 3,
              "silenceSeconds": 20,
              "switchDelaySeconds": 2,
              "rules": [
                { "when": ["BOSS"], "tracks": ["chefe"], "holdSeconds": 5 },
                { "when": ["MOBS=4"], "tracks": ["combate"], "holdSeconds": 15 },
                { "when": ["DIM=nether"], "tracks": ["nether"] },
                { "when": ["NIGHT", "RAIN||STORM"], "tracks": ["noite_chuva"] },
                { "when": ["MAIN_MENU"], "tracks": ["menu"] },
                { "when": ["DAY", "!UNDERGROUND"], "tracks": ["dia_1", "dia_2"], "interrupt": false }
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
            GoMusic.LOGGER.error("[GoMusic] Não foi possível preparar a pasta {}", ROOT, e);
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
                GoMusic.LOGGER.error("[GoMusic] Pacote inválido em {}", ROOT);
            }
        });
    }
}

package dev.gustavohrx.gomusic;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What is happening to the player right now. Rule conditions read this object.
 */
final class Situation {

    static final Set<String> FLAGS = Set.of(
            "MAIN_MENU", "CREDITS", "HOME",
            "DAY", "NIGHT", "SUNRISE", "SUNSET",
            "RAIN", "SNOW", "STORM",
            "UNDERWATER", "UNDERGROUND", "DEEP_UNDERGROUND", "HIGH_UP",
            "NEARBY_MOBS", "MINECART", "BOAT", "HORSE", "PIG",
            "FISHING", "DYING", "DAMAGED", "VILLAGE", "BOSS");

    private static final double MOB_RADIUS = 16.0;
    private static final double MOB_HEIGHT = 8.0;
    private static final double VILLAGER_RADIUS = 30.0;
    private static final double VILLAGER_HEIGHT = 15.0;
    private static final double HOME_RADIUS = 45.0;
    private static final float DYING_FRACTION = 0.35f;
    private static final int UNDERGROUND_MAX_Y = 55;
    private static final int DEEP_UNDERGROUND_MAX_Y = 15;
    private static final int HIGH_UP_MIN_Y = 128;
    private static final int DAMAGE_MEMORY_TICKS = 100;
    private static final int BOSS_MEMORY_TICKS = 10;

    private static final Gson GSON = new Gson();
    private static final Type HOMES_TYPE = new TypeToken<Map<String, double[]>>() { }.getType();

    final Set<String> flags = new HashSet<>();
    String biome = "";
    String dimension = "";
    final List<String> biomeTags = new ArrayList<>();
    int mobs;

    private long tickCount;
    private long lastDamageTick = -DAMAGE_MEMORY_TICKS;
    private long lastBossTick = -BOSS_MEMORY_TICKS;
    private float lastHealth = -1f;
    private boolean wasSleeping;
    private Map<String, double[]> homes;

    void onBossBarDrawn() {
        lastBossTick = tickCount;
    }

    /** Runs every tick so a health drop between two ticks is never missed. */
    void tick(Minecraft mc) {
        tickCount++;
        LocalPlayer player = mc.player;
        if (player == null) {
            lastHealth = -1f;
            return;
        }
        float health = player.getHealth();
        if (lastHealth >= 0f && health < lastHealth) {
            lastDamageTick = tickCount;
        }
        lastHealth = health;
    }

    /** Recalculates everything; called about once per second. */
    void refresh(Minecraft mc) {
        flags.clear();
        biomeTags.clear();
        biome = "";
        dimension = "";
        mobs = 0;

        LocalPlayer player = mc.player;
        Level level = mc.level;
        if (mc.screen instanceof WinScreen) {
            flags.add("CREDITS");
        }
        if (player == null || level == null) {
            flags.add("MAIN_MENU");
            return;
        }

        BlockPos pos = player.blockPosition();
        Holder<Biome> biomeHolder = level.getBiome(pos);
        biome = biomeHolder.unwrapKey().map(key -> key.location().toString()).orElse("");
        biomeHolder.tags().forEach(tag -> biomeTags.add(tag.location().getPath()));
        dimension = level.dimension().location().toString();

        readTime(level);
        readWeather(level, biomeHolder, pos);
        readHeight(level, player, pos);
        readEntities(level, player);
        readHome(mc, player);

        if (player.getHealth() / player.getMaxHealth() < DYING_FRACTION) {
            flags.add("DYING");
        }
        if (tickCount - lastDamageTick < DAMAGE_MEMORY_TICKS) {
            flags.add("DAMAGED");
        }
        if (tickCount - lastBossTick < BOSS_MEMORY_TICKS) {
            flags.add("BOSS");
        }
        if (player.fishing != null) {
            flags.add("FISHING");
        }

        Entity vehicle = player.getVehicle();
        if (vehicle instanceof AbstractMinecart) {
            flags.add("MINECART");
        } else if (vehicle instanceof Boat) {
            flags.add("BOAT");
        } else if (vehicle instanceof AbstractHorse) {
            flags.add("HORSE");
        } else if (vehicle instanceof Pig) {
            flags.add("PIG");
        }
    }

    private void readTime(Level level) {
        long time = level.getDayTime() % 24000L;
        flags.add(time >= 13000L && time < 23000L ? "NIGHT" : "DAY");
        if (time >= 12000L && time < 13000L) {
            flags.add("SUNSET");
        } else if (time >= 23000L) {
            flags.add("SUNRISE");
        }
    }

    private void readWeather(Level level, Holder<Biome> biomeHolder, BlockPos pos) {
        if (level.isRaining()) {
            Biome.Precipitation precipitation = biomeHolder.value().getPrecipitationAt(pos);
            if (precipitation == Biome.Precipitation.RAIN) {
                flags.add("RAIN");
            } else if (precipitation == Biome.Precipitation.SNOW) {
                flags.add("SNOW");
            }
        }
        if (level.isThundering()) {
            flags.add("STORM");
        }
    }

    private void readHeight(Level level, LocalPlayer player, BlockPos pos) {
        if (player.isUnderWater()) {
            flags.add("UNDERWATER");
        }
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }
        boolean sky = level.canSeeSky(pos);
        if (!sky && pos.getY() < UNDERGROUND_MAX_Y) {
            flags.add("UNDERGROUND");
        }
        if (!sky && pos.getY() < DEEP_UNDERGROUND_MAX_Y) {
            flags.add("DEEP_UNDERGROUND");
        }
        if (sky && pos.getY() > HIGH_UP_MIN_Y) {
            flags.add("HIGH_UP");
        }
    }

    private void readEntities(Level level, LocalPlayer player) {
        AABB mobBox = player.getBoundingBox().inflate(MOB_RADIUS, MOB_HEIGHT, MOB_RADIUS);
        mobs = level.getEntitiesOfClass(Mob.class, mobBox, mob -> mob instanceof Enemy && mob.isAlive()).size();
        if (mobs > 0) {
            flags.add("NEARBY_MOBS");
        }
        AABB villagerBox = player.getBoundingBox().inflate(VILLAGER_RADIUS, VILLAGER_HEIGHT, VILLAGER_RADIUS);
        if (!level.getEntitiesOfClass(Villager.class, villagerBox).isEmpty()) {
            flags.add("VILLAGE");
        }
    }

    /** The client does not receive the spawn point, so "home" is the last place the player slept. */
    private void readHome(Minecraft mc, LocalPlayer player) {
        String world = worldKey(mc) + "|" + dimension;
        boolean sleeping = player.isSleeping();
        if (sleeping && !wasSleeping) {
            Vec3 pos = player.position();
            loadedHomes().put(world, new double[] {pos.x, pos.y, pos.z});
            saveHomes();
        }
        wasSleeping = sleeping;

        double[] home = loadedHomes().get(world);
        if (home != null && player.position().distanceTo(new Vec3(home[0], home[1], home[2])) < HOME_RADIUS) {
            flags.add("HOME");
        }
    }

    private static String worldKey(Minecraft mc) {
        if (mc.getSingleplayerServer() != null) {
            return "local:" + mc.getSingleplayerServer().getWorldData().getLevelName();
        }
        return mc.getCurrentServer() != null ? "server:" + mc.getCurrentServer().ip : "unknown";
    }

    private Map<String, double[]> loadedHomes() {
        if (homes == null) {
            homes = new HashMap<>();
            if (Files.isRegularFile(MusicFolder.HOMES)) {
                try (Reader reader = Files.newBufferedReader(MusicFolder.HOMES, StandardCharsets.UTF_8)) {
                    Map<String, double[]> saved = GSON.fromJson(reader, HOMES_TYPE);
                    if (saved != null) {
                        homes.putAll(saved);
                    }
                } catch (IOException | RuntimeException e) {
                    GoMusic.LOGGER.warn("[GoMusic] Could not read {}", MusicFolder.HOMES, e);
                }
            }
        }
        return homes;
    }

    private void saveHomes() {
        try {
            Files.writeString(MusicFolder.HOMES, GSON.toJson(homes, HOMES_TYPE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            GoMusic.LOGGER.warn("[GoMusic] Could not save {}", MusicFolder.HOMES, e);
        }
    }
}

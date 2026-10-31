package dev.gustavohrx.gomusic;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * The rules.json file: the list of rules, in priority order.
 */
final class RuleSet {

    /** JSON format. Field names are the names used in the file. */
    private static final class Json {
        float fadeSeconds = 3f;
        float silenceSeconds = 20f;
        float switchDelaySeconds = 2f;
        List<RuleJson> rules = List.of();
    }

    private static final class RuleJson {
        List<String> when = List.of();
        List<String> tracks = List.of();
        boolean interrupt = true;
        float holdSeconds = 0f;
    }

    static final class Rule {
        final String label;
        final List<String> tracks;
        final boolean interrupt;
        final int holdTicks;
        private final List<Predicate<Situation>> conditions;
        private int holdLeft;

        private Rule(String label, List<Predicate<Situation>> conditions, List<String> tracks,
                     boolean interrupt, int holdTicks) {
            this.label = label;
            this.conditions = conditions;
            this.tracks = tracks;
            this.interrupt = interrupt;
            this.holdTicks = holdTicks;
        }

        /** Called once per refresh; holdSeconds keeps the rule valid after its conditions end. */
        boolean update(Situation situation, int elapsedTicks) {
            if (conditions.stream().allMatch(condition -> condition.test(situation))) {
                holdLeft = holdTicks;
                return true;
            }
            holdLeft = Math.max(0, holdLeft - elapsedTicks);
            return holdLeft > 0;
        }
    }

    final int fadeTicks;
    final int silenceTicks;
    final int switchDelayTicks;
    final List<Rule> rules;

    private RuleSet(int fadeTicks, int silenceTicks, int switchDelayTicks, List<Rule> rules) {
        this.fadeTicks = fadeTicks;
        this.silenceTicks = silenceTicks;
        this.switchDelayTicks = switchDelayTicks;
        this.rules = rules;
    }

    static RuleSet empty() {
        return new RuleSet(60, 400, 40, List.of());
    }

    static RuleSet load() {
        if (!Files.isRegularFile(MusicFolder.RULES)) {
            GoMusic.LOGGER.info("[GoMusic] No {}: mod inactive, keeping the game's music", MusicFolder.RULES);
            return empty();
        }
        Json json;
        try (Reader reader = Files.newBufferedReader(MusicFolder.RULES, StandardCharsets.UTF_8)) {
            json = new Gson().fromJson(reader, Json.class);
        } catch (IOException | RuntimeException e) {
            GoMusic.LOGGER.error("[GoMusic] Invalid rules.json; mod inactive", e);
            return empty();
        }
        if (json == null || json.rules == null) {
            GoMusic.LOGGER.error("[GoMusic] rules.json has no \"rules\" list; mod inactive");
            return empty();
        }

        List<Rule> rules = new ArrayList<>();
        for (RuleJson rule : json.rules) {
            Rule parsed = parse(rule);
            if (parsed != null) {
                rules.add(parsed);
            }
        }
        GoMusic.LOGGER.info("[GoMusic] Loaded {} of {} rules from the file", rules.size(), json.rules.size());
        return new RuleSet(ticks(json.fadeSeconds), ticks(json.silenceSeconds), ticks(json.switchDelaySeconds), rules);
    }

    private static Rule parse(RuleJson json) {
        String label = json.when == null || json.when.isEmpty() ? "(always)" : String.join(" + ", json.when);

        List<Predicate<Situation>> conditions = new ArrayList<>();
        try {
            if (json.when != null) {
                for (String entry : json.when) {
                    conditions.add(compileAnyOf(entry));
                }
            }
        } catch (IllegalArgumentException e) {
            GoMusic.LOGGER.error("[GoMusic] Rule \"{}\" ignored: {}", label, e.getMessage());
            return null;
        }

        List<String> tracks = new ArrayList<>();
        if (json.tracks != null) {
            for (String track : json.tracks) {
                if (MusicFolder.trackExists(track)) {
                    tracks.add(track);
                } else {
                    GoMusic.LOGGER.warn("[GoMusic] Rule \"{}\": track not found: {}.ogg", label, track);
                }
            }
        }
        if (tracks.isEmpty()) {
            GoMusic.LOGGER.error("[GoMusic] Rule \"{}\" ignored: no tracks available", label);
            return null;
        }
        return new Rule(label, conditions, tracks, json.interrupt, ticks(json.holdSeconds));
    }

    private static Predicate<Situation> compileAnyOf(String entry) {
        Predicate<Situation> result = null;
        for (String part : entry.split("\\|\\|")) {
            Predicate<Situation> condition = compile(part.trim());
            result = result == null ? condition : result.or(condition);
        }
        return result;
    }

    private static Predicate<Situation> compile(String text) {
        if (text.startsWith("!")) {
            return compile(text.substring(1).trim()).negate();
        }
        int equals = text.indexOf('=');
        if (equals < 0) {
            if (!Situation.FLAGS.contains(text)) {
                throw new IllegalArgumentException("unknown condition: " + text);
            }
            return situation -> situation.flags.contains(text);
        }

        String name = text.substring(0, equals);
        String value = text.substring(equals + 1).trim().toLowerCase(Locale.ROOT);
        switch (name) {
            case "BIOME":
                return situation -> situation.biome.contains(value);
            case "DIM":
                return situation -> situation.dimension.contains(value);
            case "BIOMETAG":
                String tag = value.startsWith("is_") ? value.substring(3) : value;
                return situation -> situation.biomeTags.stream().anyMatch(path -> path.contains(tag));
            case "MOBS":
                int minimum;
                try {
                    minimum = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("MOBS needs a number: " + text);
                }
                return situation -> situation.mobs >= minimum;
            default:
                throw new IllegalArgumentException("unknown condition: " + text);
        }
    }

    private static int ticks(float seconds) {
        return Math.max(0, Math.round(seconds * 20f));
    }
}

package dev.gustavohrx.gomusic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Decide qual faixa toca: a primeira regra válida manda, e a troca acontece com fade.
 */
final class MusicController {

    private static final int REFRESH_TICKS = 20;
    private static final int RETRY_TICKS = 40;
    private static final int FAILED_START_TICKS = 20;
    private static final int RECENT_TRACKS = 8;
    private static final int DUCK_FADE_TICKS = 30;

    private final Situation situation = new Situation();
    private final Random random = new Random();
    private final Deque<String> recentTracks = new ArrayDeque<>();
    private final List<SoundInstance> activeRecords = new ArrayList<>();

    private RuleSet ruleSet = RuleSet.empty();
    private RuleSet.Rule wantedRule;
    private RuleSet.Rule playingRule;
    private TrackSound playing;
    private int refreshCounter = REFRESH_TICKS;
    private int silenceLeft;
    private int switchWait;
    private int playingTicks;
    private Set<String> lastLoggedFlags = Set.of();

    Situation situation() {
        return situation;
    }

    boolean hasRules() {
        return !ruleSet.rules.isEmpty();
    }

    int ruleCount() {
        return ruleSet.rules.size();
    }

    void reload() {
        ruleSet = RuleSet.load();
        wantedRule = null;
        // A regra antiga não existe mais; a faixa atual termina em fade e a nova lista assume.
        playingRule = null;
        if (playing != null) {
            playing.fadeOut();
        }
    }

    void skip() {
        if (playing != null) {
            playing.fadeOut();
        }
        silenceLeft = 0;
    }

    /** Saída de emergência para uma faixa presa: reinicia o sistema de som e recomeça do zero. */
    void reset(Minecraft mc) {
        SoundManager sounds = mc.getSoundManager();
        // Fica no log para investigar a causa do travamento depois.
        GoMusic.LOGGER.warn("[GoMusic] Reset: faixa={} ativa={} saindo={} volume={} ticks={} regra={} desejada={}",
                playing != null ? playing.track : "nenhuma",
                playing != null && sounds.isActive(playing),
                playing != null && playing.isFadingOut(),
                playing != null ? playing.currentVolume() : 0f,
                playingTicks,
                playingRule != null ? playingRule.label : "-",
                wantedRule != null ? wantedRule.label : "-");

        sounds.reload();
        playing = null;
        playingRule = null;
        wantedRule = null;
        activeRecords.clear();
        silenceLeft = 0;
        switchWait = 0;
        // Relê as condições já no próximo tick, em vez de esperar até 1 s.
        refreshCounter = REFRESH_TICKS;
    }

    void onRecordStarted(SoundInstance sound) {
        activeRecords.add(sound);
    }

    void tick(Minecraft mc) {
        situation.tick(mc);
        if (mc.isPaused() || !hasRules()) {
            return;
        }

        SoundManager sounds = mc.getSoundManager();
        if (++refreshCounter >= REFRESH_TICKS) {
            situation.refresh(mc);
            if (!situation.flags.equals(lastLoggedFlags)) {
                lastLoggedFlags = Set.copyOf(situation.flags);
                GoMusic.LOGGER.debug("[GoMusic] Condições: {} | bioma: {} | tags: {} | dimensão: {} | mobs: {}",
                        lastLoggedFlags, situation.biome, situation.biomeTags, situation.dimension, situation.mobs);
            }
            wantedRule = pickRule(refreshCounter);
            refreshCounter = 0;
        }

        activeRecords.removeIf(record -> !sounds.isActive(record));
        if (playing != null) {
            updatePlaying(sounds);
        }
        if (playing == null) {
            startNextIfReady(sounds);
        }
    }

    private RuleSet.Rule pickRule(int elapsedTicks) {
        RuleSet.Rule first = null;
        // Todas atualizam, mesmo depois da primeira válida, para o holdSeconds contar certo.
        for (RuleSet.Rule rule : ruleSet.rules) {
            if (rule.update(situation, elapsedTicks) && first == null) {
                first = rule;
            }
        }
        return first;
    }

    private void updatePlaying(SoundManager sounds) {
        playingTicks++;
        if (!sounds.isActive(playing)) {
            boolean failed = playingTicks < FAILED_START_TICKS;
            boolean endedNaturally = !playing.isFadingOut();
            playing = null;
            playingRule = null;
            switchWait = 0;
            if (failed) {
                silenceLeft = RETRY_TICKS;
            } else if (endedNaturally) {
                silenceLeft = ruleSet.silenceTicks;
            }
            return;
        }

        playing.setDucked(!activeRecords.isEmpty());
        if (playing.isFadingOut() || wantedRule == playingRule) {
            switchWait = 0;
            return;
        }
        if (wantedRule != null && wantedRule.tracks.contains(playing.track)) {
            // A regra nova também usa esta faixa: troca a regra sem reiniciar a música.
            playingRule = wantedRule;
            switchWait = 0;
            return;
        }
        if (wantedRule != null && !wantedRule.interrupt) {
            return;
        }
        if (++switchWait >= ruleSet.switchDelayTicks) {
            playing.fadeOut();
            switchWait = 0;
        }
    }

    private void startNextIfReady(SoundManager sounds) {
        // Regra de maior prioridade não espera o silêncio entre faixas de quem tocava antes.
        if (wantedRule == null) {
            return;
        }
        if (silenceLeft > 0) {
            silenceLeft--;
            return;
        }
        String track = pickTrack(wantedRule.tracks);
        playing = new TrackSound(track, ruleSet.fadeTicks > 0 ? ruleSet.fadeTicks : DUCK_FADE_TICKS);
        playingRule = wantedRule;
        playingTicks = 0;
        sounds.play(playing);
        GoMusic.LOGGER.info("[GoMusic] Tocando {} (regra: {})", track, wantedRule.label);
    }

    private String pickTrack(List<String> tracks) {
        List<String> fresh = new ArrayList<>(tracks);
        fresh.removeAll(recentTracks);
        List<String> choices = fresh.isEmpty() ? tracks : fresh;
        String picked = choices.get(random.nextInt(choices.size()));

        recentTracks.remove(picked);
        recentTracks.addLast(picked);
        if (recentTracks.size() > RECENT_TRACKS) {
            recentTracks.removeFirst();
        }
        return picked;
    }

    List<String> describe() {
        List<String> lines = new ArrayList<>();
        lines.add("[GoMusic] " + ruleSet.rules.size() + " regras"
                + (hasRules() ? "" : " (inativo: falta gomusic/rules.json)"));
        lines.add("Faixa: " + (playing != null ? playing.track : "nenhuma")
                + (playing != null && playing.isFadingOut() ? " (saindo)" : "")
                + " | regra atual: " + (playingRule != null ? playingRule.label : "-")
                + " | regra desejada: " + (wantedRule != null ? wantedRule.label : "-"));
        lines.add("Condições: " + situation.flags);
        lines.add("Bioma: " + situation.biome + " | tags: " + situation.biomeTags);
        lines.add("Dimensão: " + situation.dimension + " | mobs: " + situation.mobs);
        return lines;
    }
}

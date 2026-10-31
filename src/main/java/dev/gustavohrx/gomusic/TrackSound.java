package dev.gustavohrx.gomusic;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.ConstantFloat;

/**
 * A track played by the game's sound system, in the Music category, without needing sounds.json.
 */
final class TrackSound extends AbstractTickableSoundInstance {

    final String track;
    private final float fadeStep;
    private boolean fadingOut;
    private boolean ducked;

    TrackSound(String track, int fadeTicks) {
        super(SoundEvent.createVariableRangeEvent(new ResourceLocation(GoMusic.MOD_ID, track)),
                SoundSource.MUSIC, SoundInstance.createUnseededRandom());
        this.track = track;
        this.fadeStep = 1f / Math.max(1, fadeTicks);
        this.volume = 0f;
        this.looping = false;
        this.relative = true;
        this.attenuation = Attenuation.NONE;
    }

    @Override
    public WeighedSoundEvents resolve(SoundManager manager) {
        // stream = true: reads from disk gradually instead of loading the whole track into memory.
        this.sound = new Sound(location.toString(), ConstantFloat.of(1f), ConstantFloat.of(1f),
                1, Sound.Type.FILE, true, false, 16);
        WeighedSoundEvents events = new WeighedSoundEvents(location, null);
        events.addSound(sound);
        return events;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        float target = fadingOut || ducked ? 0f : 1f;
        if (volume < target) {
            volume = Math.min(target, volume + fadeStep);
        } else if (volume > target) {
            volume = Math.max(target, volume - fadeStep);
        }
        if (fadingOut && volume <= 0f) {
            stop();
        }
    }

    void fadeOut() {
        fadingOut = true;
    }

    boolean isFadingOut() {
        return fadingOut;
    }

    float currentVolume() {
        return volume;
    }

    void setDucked(boolean ducked) {
        this.ducked = ducked;
    }
}

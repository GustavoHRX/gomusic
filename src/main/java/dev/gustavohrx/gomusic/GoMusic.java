package dev.gustavohrx.gomusic;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(GoMusic.MOD_ID)
public class GoMusic {

    public static final String MOD_ID = "gomusic";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GoMusic() {
        // O mod só existe no cliente; em servidor dedicado não faz nada.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.init();
        }
    }
}

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
        // Client-only mod: it does nothing on a dedicated server.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.init();
        }
    }
}

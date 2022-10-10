package showcase

import com.github.dwursteisen.minigdx.GameApplicationBuilder
import com.github.dwursteisen.minigdx.GameConfiguration
import com.github.dwursteisen.minigdx.GameScreenConfiguration
import com.github.dwursteisen.minigdx.Window
import com.github.minigdx.showcase.Particles
import com.github.minigdx.showcase.cameras.CamerasGame
import com.github.minigdx.showcase.dance.DanceGame
import com.github.minigdx.showcase.imguilight.ImGUILight
import com.github.minigdx.showcase.light.LightGame
import com.github.minigdx.showcase.physic.SatTank
import com.github.minigdx.showcase.shader.GhostShader
import com.github.minigdx.showcase.text.DebugText
import com.github.minigdx.showcase.text.SampleText
import com.github.minigdx.showcase.treed.PlatformerGame3D
import com.github.minigdx.showcase.twod.PlatformerGame2D

class Main {

    companion object {

        @JvmStatic
        fun main(vararg args: String) {
            GameApplicationBuilder(
                gameFactory = {
                    PlatformerGame2D(it)
                },
                gameConfigurationFactory = {
                    GameConfiguration(
                        "MiniGDX Showcase",
                        gameScreenConfiguration = GameScreenConfiguration.WithRatio(16f / 9f),
                        debug = false,
                        jointLimit = 100,
                        window = Window(800, 450, "minigdx")
                    )

                }
            ).start()
        }
    }
}

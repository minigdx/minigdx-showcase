package showcase

import com.github.dwursteisen.minigdx.GameApplicationBuilder
import com.github.dwursteisen.minigdx.GameConfiguration
import com.github.dwursteisen.minigdx.GameScreenConfiguration
import com.github.dwursteisen.minigdx.Window
import com.github.minigdx.showcase.Particles
import com.github.minigdx.showcase.dance.DanceGame
import com.github.minigdx.showcase.imguilight.ImGUILight
import com.github.minigdx.showcase.physic.SatTank
import com.github.minigdx.showcase.text.DebugText

class Main {

    companion object {

        @JvmStatic
        fun main(vararg args: String) {
            GameApplicationBuilder(
                gameFactory = {
                    DanceGame(it)
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

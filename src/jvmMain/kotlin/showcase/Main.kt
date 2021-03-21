package showcase

import com.github.dwursteisen.minigdx.GameApplicationBuilder
import com.github.dwursteisen.minigdx.GameConfiguration
import com.github.dwursteisen.minigdx.GameScreenConfiguration
import com.github.dwursteisen.minigdx.Window
import com.github.minigdx.showcase.twod.platform.PlatformerGame2D

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
                        gameScreenConfiguration = GameScreenConfiguration.WithRatio(16f/9f),
                        debug = false,
                        window = Window(1024, (1024 * 9f/16f).toInt(), "migdx")
                    )

                }
            ).start()
        }
    }
}

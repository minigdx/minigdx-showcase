package showcase

import com.github.dwursteisen.minigdx.GameApplicationBuilder
import com.github.dwursteisen.minigdx.GameConfiguration
import com.github.dwursteisen.minigdx.GameScreenConfiguration
import com.github.dwursteisen.minigdx.Seconds
import com.github.dwursteisen.minigdx.Window
import com.github.dwursteisen.minigdx.ecs.components.Component
import com.github.dwursteisen.minigdx.ecs.entities.Entity
import com.github.dwursteisen.minigdx.ecs.systems.EntityQuery
import com.github.dwursteisen.minigdx.ecs.systems.System
import com.github.minigdx.showcase.dance.DanceGame
import com.github.minigdx.showcase.physic.SatTank
import com.github.minigdx.showcase.treed.platform.PlatformerGame3D
import com.github.minigdx.showcase.twod.platform.PlatformerGame2D

class Main {

    companion object {

        @JvmStatic
        fun main(vararg args: String) {
            GameApplicationBuilder(
                gameFactory = {
                    SatTank(it)
                },
                gameConfigurationFactory = {
                    GameConfiguration(
                        "MiniGDX Showcase",
                        gameScreenConfiguration = GameScreenConfiguration.WithRatio(16f/9f),
                        debug = true,
                        window = Window(1024, (1024 * 9f/16f).toInt(), "minigdx")
                    )

                }
            ).start()
        }
    }
}

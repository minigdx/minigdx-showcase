package com.github.minigdx.showcase

import com.github.dwursteisen.minigdx.GameApplicationBuilder
import com.github.dwursteisen.minigdx.GameConfiguration
import com.github.dwursteisen.minigdx.GameScreenConfiguration
import com.github.minigdx.showcase.treed.platform.PlatformerGame3D
import com.github.minigdx.showcase.twod.platform.PlatformerGame2D
import kotlinx.browser.document
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.get

fun main() {
    val canvas = document.getElementsByTagName("canvas")[0]!!
    GameApplicationBuilder(
        gameConfigurationFactory = {
            GameConfiguration(
                gameName = "Showcase game",
                debug = false,
                canvas = canvas as HTMLCanvasElement,
                gameScreenConfiguration = GameScreenConfiguration.WithRatio(16f / 9f),
                rootPath = "/js/distributions/"
            )
        },
        gameFactory = {
            val gameName = canvas.getAttribute("property").toString()
            when(gameName) {
                "2d-platformer" -> PlatformerGame2D(it)
                "3d-platformer" -> PlatformerGame3D(it)
                else -> TODO()
            }
        }
    ).start()
}

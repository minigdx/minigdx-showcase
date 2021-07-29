package com.github.minigdx.showcase.imguilight

import com.github.dwursteisen.minigdx.GameContext
import com.github.dwursteisen.minigdx.ecs.Engine
import com.github.dwursteisen.minigdx.ecs.entities.EntityFactory
import com.github.dwursteisen.minigdx.ecs.systems.System
import com.github.dwursteisen.minigdx.file.Texture
import com.github.dwursteisen.minigdx.game.Game
import com.github.dwursteisen.minigdx.imgui.ImGuiSystem
import com.github.minigdx.imgui.WidgetBuilder

class ImGUILight(override val gameContext: GameContext) : Game {

    override fun createEntities(entityFactory: EntityFactory) = Unit

    override fun createSystems(engine: Engine): List<System> {
        return listOf(object : ImGuiSystem() {

            var label = "Hello"

            override fun gui(builder: WidgetBuilder<Texture>) {
                builder.verticalContainer(width = 0.2f) {
                    button(label = label) {
                        label = if (label == "Hello") {
                            "World!"
                        } else {
                            "Hello"
                        }
                    }
                }
            }
        })
    }
}

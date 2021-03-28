package com.github.minigdx.showcase.dance

import com.dwursteisen.minigdx.scene.api.Scene
import com.dwursteisen.minigdx.scene.api.relation.ObjectType
import com.github.dwursteisen.minigdx.GameContext
import com.github.dwursteisen.minigdx.ecs.entities.EntityFactory
import com.github.dwursteisen.minigdx.ecs.entities.position
import com.github.dwursteisen.minigdx.file.get
import com.github.dwursteisen.minigdx.game.Game

class DanceGame(override val gameContext: GameContext) : Game {

    private val scene: Scene by gameContext.fileHandler.get("dance.protobuf")

    override fun createEntities(entityFactory: EntityFactory) {
        scene.children.forEach {
            val e = entityFactory.createFromNode(it, scene)
            if(it.type == ObjectType.LIGHT) {
                e.position.setGlobalTranslation(1.4f, 1.75f, 2.8f)
            }
        }
    }
}

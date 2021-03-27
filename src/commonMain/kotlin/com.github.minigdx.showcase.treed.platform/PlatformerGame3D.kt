package com.github.minigdx.showcase.treed.platform

import com.dwursteisen.minigdx.scene.api.Scene
import com.github.dwursteisen.minigdx.GameContext
import com.github.dwursteisen.minigdx.Seconds
import com.github.dwursteisen.minigdx.ecs.Engine
import com.github.dwursteisen.minigdx.ecs.components.Component
import com.github.dwursteisen.minigdx.ecs.components.Position
import com.github.dwursteisen.minigdx.ecs.entities.Entity
import com.github.dwursteisen.minigdx.ecs.entities.EntityFactory
import com.github.dwursteisen.minigdx.ecs.entities.position
import com.github.dwursteisen.minigdx.ecs.systems.EntityQuery
import com.github.dwursteisen.minigdx.ecs.systems.System
import com.github.dwursteisen.minigdx.file.get
import com.github.dwursteisen.minigdx.game.Game

class Root : Component

class RootSystem : System(EntityQuery(Root::class)) {

    override fun update(delta: Seconds, entity: Entity) {
        entity.position.addGlobalRotation(y = 45f, delta = delta)
    }
}
class PlatformerGame3D(override val gameContext: GameContext) : Game {

    private val scene: Scene by gameContext.fileHandler.get("3d-platformer.protobuf")

    override fun createEntities(entityFactory: EntityFactory) {
        val root = entityFactory.create {
            add(Position())
            add(Root())
        }
        scene.children.forEach {
            entityFactory.createFromNode(it, scene).attachTo(root)
        }
    }

    override fun createSystems(engine: Engine): List<System> {
        return super.createSystems(engine) + listOf(RootSystem())
    }
}

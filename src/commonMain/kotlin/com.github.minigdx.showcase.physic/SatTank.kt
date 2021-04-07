package com.github.minigdx.showcase.physic

import com.dwursteisen.minigdx.scene.api.Scene
import com.github.dwursteisen.minigdx.GameContext
import com.github.dwursteisen.minigdx.Seconds
import com.github.dwursteisen.minigdx.ecs.Engine
import com.github.dwursteisen.minigdx.ecs.components.Component
import com.github.dwursteisen.minigdx.ecs.components.ScriptComponent
import com.github.dwursteisen.minigdx.ecs.components.gl.BoundingBox
import com.github.dwursteisen.minigdx.ecs.entities.Entity
import com.github.dwursteisen.minigdx.ecs.entities.EntityFactory
import com.github.dwursteisen.minigdx.ecs.entities.position
import com.github.dwursteisen.minigdx.ecs.physics.SATCollisionResolver
import com.github.dwursteisen.minigdx.ecs.script.ScriptContext
import com.github.dwursteisen.minigdx.ecs.systems.EntityQuery
import com.github.dwursteisen.minigdx.ecs.systems.System
import com.github.dwursteisen.minigdx.file.get
import com.github.dwursteisen.minigdx.game.Game
import com.github.dwursteisen.minigdx.input.Key
import com.github.dwursteisen.minigdx.math.Interpolations
import com.github.dwursteisen.minigdx.math.Vector3
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class Tank(
    var currentSpeed: Float = 0f,
    var currentDirection: Vector3 = Vector3(0f, 0f, 1f)
) : Component

class Cube : Component

class Energie : Component

class TankSystem : System(EntityQuery(Tank::class)) {

    private val satCollisionResolver = SATCollisionResolver()

    private val energie by interested(EntityQuery(Energie::class))

    private val cubes by interested(EntityQuery(Cube::class))

    override fun update(delta: Seconds, entity: Entity) {
        val tank = entity.get(Tank::class)
        if (input.isKeyPressed(Key.ARROW_UP)) {
            tank.currentSpeed = min(tank.currentSpeed + 2f * delta, 2f)
        } else if (input.isKeyPressed(Key.ARROW_DOWN)) {
            tank.currentSpeed = max(tank.currentSpeed - 2f * delta, -1f)
        } else {
            tank.currentSpeed = Interpolations.lerp(0f, tank.currentSpeed)
        }

        if (input.isKeyPressed(Key.ARROW_LEFT)) {
            entity.position.addLocalRotation(y = 45f * delta)
            tank.currentDirection.rotate(0f, 1f, 0f, 45f * delta)
        } else if (input.isKeyPressed(Key.ARROW_RIGHT)) {
            entity.position.addLocalRotation(y = -45f * delta)
            tank.currentDirection.rotate(0f, 1f, 0f, -45f * delta)
        }

        // Copy the list as removing an entity might lead to a concurrent modification exception.
        energie.toMutableList().forEach {
            if (satCollisionResolver.collide(entity, it)) {
                it.get(BoundingBox::class).touch = true
                entity.get(BoundingBox::class).touch  = true
                it.add(ScriptComponent(
                    script = {
                        destroyBox(it)
                    }
                ))
                it.remove(Energie::class)
            }
        }
        val speed = tank.currentDirection.copy().scale(-tank.currentSpeed * delta)
        entity.position.addLocalTranslation(speed.x, speed.y, speed.z)
    }

    private suspend fun ScriptContext.destroyBox(target: Entity) {
        var duration = 1f
        while(duration > 0f) {
            duration -= delta
            target.position.addWorldRotation(y = 180f, delta = delta)
            yield()
        }
        target.destroy()
    }
}

class SatTank(override val gameContext: GameContext) : Game {

    private val scene by gameContext.fileHandler.get<Scene>("sat-collision.protobuf")

    override fun createEntities(entityFactory: EntityFactory) {
        scene.children.forEach {
            val e = entityFactory.createFromNode(it, scene)
            if (it.name == "tank") {
                e.add(Tank())
            }
            if (it.name.startsWith("Cube")) {
                e.add(Cube())
            }
            if (it.name.startsWith("enj")) {
                e.add(Energie())
            }
        }
    }

    override fun createSystems(engine: Engine): List<System> {
        return listOf<System>(TankSystem())
    }
}

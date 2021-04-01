package com.github.minigdx.showcase.treed.platform

import com.curiouscreature.kotlin.math.Quaternion
import com.dwursteisen.minigdx.scene.api.Scene
import com.dwursteisen.minigdx.scene.api.relation.ObjectType
import com.github.dwursteisen.minigdx.GameContext
import com.github.dwursteisen.minigdx.Seconds
import com.github.dwursteisen.minigdx.api.toMat4
import com.github.dwursteisen.minigdx.ecs.Engine
import com.github.dwursteisen.minigdx.ecs.components.Camera
import com.github.dwursteisen.minigdx.ecs.components.Component
import com.github.dwursteisen.minigdx.ecs.components.Position
import com.github.dwursteisen.minigdx.ecs.components.SpriteComponent
import com.github.dwursteisen.minigdx.ecs.components.gl.BoundingBox
import com.github.dwursteisen.minigdx.ecs.entities.Entity
import com.github.dwursteisen.minigdx.ecs.entities.EntityFactory
import com.github.dwursteisen.minigdx.ecs.entities.position
import com.github.dwursteisen.minigdx.ecs.physics.RayResolver
import com.github.dwursteisen.minigdx.ecs.states.State
import com.github.dwursteisen.minigdx.ecs.systems.EntityQuery
import com.github.dwursteisen.minigdx.ecs.systems.StateMachineSystem
import com.github.dwursteisen.minigdx.ecs.systems.System
import com.github.dwursteisen.minigdx.file.get
import com.github.dwursteisen.minigdx.game.Game
import com.github.dwursteisen.minigdx.input.Key
import com.github.dwursteisen.minigdx.math.Interpolations
import com.github.dwursteisen.minigdx.math.Vector3
import com.github.minigdx.showcase.treed.platform.CollisionUtils.platformHit
import com.github.minigdx.showcase.twod.platform.Platform
import com.github.minigdx.showcase.twod.platform.Player
import com.github.minigdx.showcase.twod.platform.createSprite
import kotlin.math.sqrt

class Root : Component

class RootSystem : System(EntityQuery(Root::class)) {

    val player by interested(EntityQuery(Player::class))

    val platforms by interested(EntityQuery(Platform::class))

    override fun update(delta: Seconds, entity: Entity) {
        val box = player.first().get(BoundingBox::class)
        if (input.isKeyPressed(Key.ARROW_RIGHT)) {
            val closestHit = platformHit(
                platforms,
                Vector3(box.max.x, box.center.y, box.center.z),
                Vector3.X
            )
            if (closestHit == null) {
                entity.position.addGlobalRotation(y = -90f, delta = delta)
            }
        } else if (input.isKeyPressed(Key.ARROW_LEFT)) {
            val closestHit = platformHit(
                platforms,
                Vector3(box.min.x, box.center.y, box.center.z),
                Vector3.MINUS_X
            )
            if (closestHit == null) {
                entity.position.addGlobalRotation(y = 90f, delta = delta)
            }
        }
    }
}

class CameraSystem : System(EntityQuery(Camera::class)) {

    val player by interested(EntityQuery(Player::class))

    override fun update(delta: Seconds, entity: Entity) {
        entity.position.setGlobalTranslation(
            y = Interpolations.lerp(
                player.first().position.translation.y,
                entity.position.translation.y
            )
        )
    }
}

class PlayerSystem : StateMachineSystem(Player::class) {

    val platforms by interested(EntityQuery(Platform::class))

    class Idle(val parent: PlayerSystem) : State() {

        override fun onEnter(entity: Entity) {
            entity.get(SpriteComponent::class).switchToAnimation("idle")
        }

        override fun update(delta: Seconds, entity: Entity): State? {
            if (parent.input.isAnyKeysPressed(Key.ARROW_LEFT, Key.ARROW_RIGHT)) {
                return Run(parent)
            }
            if (parent.input.isKeyPressed(Key.SPACE)) {
                return Jump(parent)
            }
            return null
        }
    }

    class Run(val parent: PlayerSystem) : State() {

        override fun onEnter(entity: Entity) {
            entity.get(SpriteComponent::class).switchToAnimation("run")
        }

        override fun update(delta: Seconds, entity: Entity): State? {
            if (parent.input.isKeyPressed(Key.SPACE)) {
                return Jump(parent)
            }
            if (parent.move(entity)) {
                val box = entity.get(BoundingBox::class)
                val closestHit = platformHit(
                    parent.platforms,
                    Vector3(box.center.x, box.min.y, box.center.z),
                    Vector3.MINUS_Y
                )
                return if (closestHit == null) {
                    // No platform under, let's fall
                    Jump(parent, initialVelocity = 0f)
                } else {
                    // There is still a platform under us
                    null
                }
            }

            return Idle(parent)
        }
    }

    class Jump(val parent: PlayerSystem, initialVelocity: Float? = null) : State() {

        private var velocity = initialVelocity ?: sqrt(GRAVITY * JUMP_HEIGHT)

        override fun onEnter(entity: Entity) {
            entity.get(SpriteComponent::class).switchToAnimation("jump_up")
        }

        override fun update(delta: Seconds, entity: Entity): State? {
            val position = entity.get(Position::class)
            if (velocity < 0f) {
                entity.get(SpriteComponent::class).switchToAnimation("jump_down")
                val box = entity.get(BoundingBox::class)
                val closestHit = platformHit(
                    parent.platforms,
                    Vector3(box.center.x, box.min.y, box.center.z),
                    Vector3.MINUS_Y
                )

                if (closestHit != null) {
                    val (platform, hit) = closestHit
                    val result = platform.max.y - hit.y
                    entity.get(Position::class).addGlobalTranslation(y = result)
                    return Idle(parent)
                }
            }

            parent.move(entity)
            position.addGlobalTranslation(y = velocity, delta = delta)
            velocity -= GRAVITY * delta
            return null
        }

        companion object {

            const val JUMP_HEIGHT = 1.5f // World unit
            const val JUMP_DURATION = 0.2f // Duration of the jump
            const val GRAVITY = JUMP_HEIGHT / (JUMP_DURATION * JUMP_DURATION)
        }
    }

    private fun move(entity: Entity): Boolean {
        val position = entity.position
        return if (input.isKeyPressed(Key.ARROW_LEFT)) {
            position.setGlobalRotation(Quaternion.fromEulers(0f, 1f, 0f, 180f))
            true
        } else if (input.isKeyPressed(Key.ARROW_RIGHT)) {
            position.setGlobalRotation(Quaternion.fromEulers(0f, 1f, 0f, 0f))
            true
        } else {
            false
        }
    }

    override fun initialState(entity: Entity): State {
        return Idle(this)
    }
}

object CollisionUtils {

    private val rayResolver = RayResolver()

    fun platformHit(
        platforms: List<Entity>,
        startPoint: Vector3,
        direction: Vector3
    ): Pair<BoundingBox, Vector3>? {
        val map = platforms.mapNotNull { platform ->

            val intersectRayBounds = rayResolver.intersectRayBounds(
                startPoint,
                direction,
                platform
            )
            if (intersectRayBounds == null) {
                null
            } else {
                platform.get(BoundingBox::class) to intersectRayBounds
            }
        }
        return map
            .filter { (_, hit) -> hit.dist2(startPoint) <= 0.1 * 0.1f }
            .onEach { (p, _) -> p.touch = true }
            .map { (p, _) -> p to startPoint }
            .firstOrNull()
    }
}

class PlatformerGame3D(override val gameContext: GameContext) : Game {

    private val scene: Scene by gameContext.fileHandler.get("3d-platformer-tower.protobuf")

    private val spr: Scene by gameContext.fileHandler.get("2d-platformer-spr.protobuf")

    override fun createEntities(entityFactory: EntityFactory) {
        val root = entityFactory.create {
            add(Position())
            add(Root())
        }
        scene.children.forEach {

            if (it.name == "player") {
                val player = entityFactory.engine.createSprite(
                    spr.sprites.values.first(),
                    spr,
                    it.transformation.toMat4()
                ).also { e ->
                    e.name = it.name
                }

                // bounding box
                val box = entityFactory.createBox(it, scene).attachTo(player)
                player.add(Player())
                player.add(box.get(BoundingBox::class))
                box.destroy()
            } else if (it.name.startsWith("platform")) {
                val platform = entityFactory.createFromNode(it, scene)
                platform.add(Platform())
                val model = scene.models.get(it.reference)
                model?.let {
                    platform.add(BoundingBox.from(model.mesh))
                }
                platform.attachTo(root)
            } else {
                val e = entityFactory.createFromNode(it, scene)
                if (it.type == ObjectType.MODEL) {
                    e.attachTo(root)
                }
            }
        }
    }

    override fun createSystems(engine: Engine): List<System> {
        return listOf(RootSystem(), CameraSystem(), PlayerSystem())
    }
}

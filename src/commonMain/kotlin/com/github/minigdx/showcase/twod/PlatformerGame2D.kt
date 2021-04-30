package com.github.minigdx.showcase.twod

import com.curiouscreature.kotlin.math.Mat4
import com.curiouscreature.kotlin.math.Quaternion.Companion.fromEulers
import com.dwursteisen.minigdx.scene.api.Scene
import com.dwursteisen.minigdx.scene.api.common.Id
import com.dwursteisen.minigdx.scene.api.model.Normal
import com.dwursteisen.minigdx.scene.api.model.Primitive
import com.dwursteisen.minigdx.scene.api.model.UV
import com.dwursteisen.minigdx.scene.api.model.Vertex
import com.dwursteisen.minigdx.scene.api.sprite.Sprite
import com.github.dwursteisen.minigdx.GameContext
import com.github.dwursteisen.minigdx.Seconds
import com.github.dwursteisen.minigdx.api.toMat4
import com.github.dwursteisen.minigdx.ecs.Engine
import com.github.dwursteisen.minigdx.ecs.components.Component
import com.github.dwursteisen.minigdx.ecs.components.Position
import com.github.dwursteisen.minigdx.ecs.components.SpriteComponent
import com.github.dwursteisen.minigdx.ecs.components.StateMachineComponent
import com.github.dwursteisen.minigdx.ecs.components.gl.BoundingBox
import com.github.dwursteisen.minigdx.ecs.components.gl.MeshPrimitive
import com.github.dwursteisen.minigdx.ecs.entities.Entity
import com.github.dwursteisen.minigdx.ecs.entities.EntityFactory
import com.github.dwursteisen.minigdx.ecs.entities.position
import com.github.dwursteisen.minigdx.ecs.physics.AABBCollisionResolver
import com.github.dwursteisen.minigdx.ecs.physics.RayResolver
import com.github.dwursteisen.minigdx.ecs.states.State
import com.github.dwursteisen.minigdx.ecs.systems.EntityQuery
import com.github.dwursteisen.minigdx.ecs.systems.StateMachineSystem
import com.github.dwursteisen.minigdx.ecs.systems.System
import com.github.dwursteisen.minigdx.file.get
import com.github.dwursteisen.minigdx.game.Game
import com.github.dwursteisen.minigdx.input.Key
import com.github.dwursteisen.minigdx.math.Vector3
import kotlin.math.sqrt

class Player(var direction: Float = 1f) : StateMachineComponent()
class Coin : Component
class CoinHixBox : Component
class Platform : Component

class CoinHitBoxSystem : System(EntityQuery(CoinHixBox::class)) {

    private val collider = AABBCollisionResolver()

    private val player by interested(EntityQuery(Player::class))

    override fun update(delta: Seconds, entity: Entity) {
        if (collider.collide(
                entity,
                player.first().getChild("body")
            )
        ) {
            // TODO: play a sound?
            entity.destroy()
        }
    }
}

class CoinSystem : System(EntityQuery(Coin::class)) {

    override fun update(delta: Seconds, entity: Entity) {
        entity.get(Position::class).addLocalRotation(y = 90f, delta = delta)
    }
}

class PlayerSystem : StateMachineSystem(Player::class) {

    private val platforms by interested(EntityQuery(Platform::class))

    private val rayResolver = RayResolver()

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
            if (parent.move(entity, delta)) {
                val closestHit = parent.platformHit(entity, parent.platforms)
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

                val closestHit = parent.platformHit(entity, parent.platforms)

                if (closestHit != null) {
                    val (platform, hit) = closestHit
                    val result = platform.max.y - hit.y
                    entity.get(Position::class).addGlobalTranslation(y = result)
                    return Idle(parent)
                }
            }

            parent.move(entity, delta)
            position.addGlobalTranslation(y = velocity, delta = delta)
            velocity -= GRAVITY * delta
            return null
        }

        companion object {

            const val JUMP_HEIGHT = 2.5f // World unit
            const val JUMP_DURATION = 0.2f // Duration of the jump
            const val GRAVITY = JUMP_HEIGHT / (JUMP_DURATION * JUMP_DURATION)
        }
    }

    private fun move(entity: Entity, delta: Seconds): Boolean {
        val position = entity.position
        return if (input.isKeyPressed(Key.ARROW_LEFT)) {
            // still in the screen limit
            if (position.translation.x - (5f * delta) > -6.5f) {
                position.addGlobalTranslation(x = -5f, delta = delta)
            }
            position.setWorldRotation(fromEulers(0f, 1f, 0f, 180f))
            true
        } else if (input.isKeyPressed(Key.ARROW_RIGHT)) {
            // still in the screen limit
            if (position.translation.x + (5f * delta) < 6.5f) {
                position.addGlobalTranslation(x = 5f, delta = delta)
            }
            position.setWorldRotation(fromEulers(0f, 1f, 0f, 0f))
            true
        } else {
            false
        }
    }

    private fun platformHit(
        player: Entity,
        platforms: List<Entity>
    ): Pair<BoundingBox, Vector3>? {
        val base = player.getChild("base")
        val box = base.get(BoundingBox::class)

        val lowerLeft = Vector3(box.center.x, box.min.y, box.center.z)

        val map = platforms.mapNotNull { platform ->

            val intersectRayBounds = rayResolver.intersectRayBounds(
                lowerLeft,
                Vector3.MINUS_Y,
                platform
            )
            if (intersectRayBounds == null) {
                null
            } else {
                platform.get(BoundingBox::class) to intersectRayBounds
            }
        }
        return map
            .filter { (_, hit) -> hit.dist2(lowerLeft) <= 0.1 * 0.1f }
            .map { (p, _) -> p to lowerLeft }
            .firstOrNull()
    }

    override fun initialState(entity: Entity): State {
        return Idle(this)
    }
}

class PlatformerGame2D(override val gameContext: GameContext) : Game {

    private val scene: Scene by gameContext.fileHandler.get("2d-platformer.protobuf")
    private val spr: Scene by gameContext.fileHandler.get("2d-platformer-spr.protobuf")

    override fun createEntities(entityFactory: EntityFactory) {
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
                it.children.forEach { child ->
                    entityFactory.createBox(child).attachTo(player)
                }
                player.add(Player())
            } else if (it.name.startsWith("coin")) {
                val hitbox = entityFactory.createBox(it)
                hitbox.add(CoinHixBox())

                val sprite = entityFactory.engine.createSprite(
                    spr.sprites.values.first(),
                    spr,
                    it.children.first().transformation.toMat4()
                )
                    .also { e ->
                        e.name = it.name

                    }
                sprite.get(SpriteComponent::class).switchToAnimation("coin")
                sprite.add(Coin())
                sprite.attachTo(hitbox)
            } else if (it.name.startsWith("platform")) {
                val platform = entityFactory.createFromNode(it, scene)
                platform.add(Platform())
            } else {
                entityFactory.createFromNode(it, scene)
            }
        }
    }

    override fun createSystems(engine: Engine): List<System> {
        return listOf(CoinHitBoxSystem(), CoinSystem(), PlayerSystem())
    }
}

// TODO: put that in the EntityFactory
fun Engine.createSprite(sprite: Sprite, scene: Scene, transformation: Mat4): Entity = create {
    add(Position(transformation, transformation, transformation))
    add(
        SpriteComponent(
            animations = sprite.animations,
            uvs = sprite.uvs
        )
    )
    add(
        MeshPrimitive(
            id = Id(),
            name = "undefined",
            material = scene.materials.getValue(sprite.materialReference),
            hasAlpha = scene.materials.getValue(sprite.materialReference).hasAlpha,
            primitive = Primitive(
                id = Id(),
                materialId = sprite.materialReference,
                vertices = listOf(
                    Vertex(
                        com.dwursteisen.minigdx.scene.api.model.Position(-1f, -1f, 0f),
                        Normal(0f, 0f, 0f),
                        uv = UV(0f, 0f)
                    ),
                    Vertex(
                        com.dwursteisen.minigdx.scene.api.model.Position(1f, -1f, 0f),
                        Normal(0f, 0f, 0f),
                        uv = UV(0f, 0f)
                    ),
                    Vertex(
                        com.dwursteisen.minigdx.scene.api.model.Position(-1f, 1f, 0f),
                        Normal(0f, 0f, 0f),
                        uv = UV(0f, 0f)
                    ),
                    Vertex(
                        com.dwursteisen.minigdx.scene.api.model.Position(1f, 1f, 0f),
                        Normal(0f, 0f, 0f),
                        uv = UV(0f, 0f)
                    )
                ),
                verticesOrder = intArrayOf(
                    0,
                    1,
                    2,
                    2,
                    1,
                    3
                )
            )
        )
    )
}

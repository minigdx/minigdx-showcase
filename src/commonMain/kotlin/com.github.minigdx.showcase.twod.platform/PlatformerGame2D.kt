package com.github.minigdx.showcase.twod.platform

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
import com.github.dwursteisen.minigdx.ecs.states.State
import com.github.dwursteisen.minigdx.ecs.systems.EntityQuery
import com.github.dwursteisen.minigdx.ecs.systems.StateMachineSystem
import com.github.dwursteisen.minigdx.ecs.systems.System
import com.github.dwursteisen.minigdx.file.get
import com.github.dwursteisen.minigdx.game.Game
import com.github.dwursteisen.minigdx.input.Key
import kotlin.math.sqrt

class Player(var base: BoundingBox) : StateMachineComponent()
class Coin : Component
class Platform : Component

class CoinSystem : System(EntityQuery(Coin::class)) {

    private val collider = AABBCollisionResolver()

    private val player by interested(EntityQuery(Player::class))

    override fun update(delta: Seconds, entity: Entity) {
        entity.get(Position::class).addLocalRotation(y = 90f, delta = delta)
        if (entity.hasComponent(BoundingBox::class) && collider.collide(
                entity,
                entity.position.globalTransformation,
                player.first()
            )
        ) {
            // TODO: play a sound?
            entity.destroy()
        }
    }
}

class PlayerSystem : StateMachineSystem(Player::class) {

    private val platforms by interested(EntityQuery(Platform::class))

    private val collider = AABBCollisionResolver()

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
                return null
            }

            return Idle(parent)
        }
    }

    class Jump(val parent: PlayerSystem) : State() {

        private val gravity = JUMP_HEIGHT / (JUMP_DURATION * JUMP_DURATION)
        private var velocity = sqrt(gravity * JUMP_HEIGHT)

        override fun onEnter(entity: Entity) {
            entity.get(SpriteComponent::class).switchToAnimation("jump_up")
        }

        override fun update(delta: Seconds, entity: Entity): State? {
            val position = entity.get(Position::class)
            if (velocity < 0f) {
                entity.get(SpriteComponent::class).switchToAnimation("jump_down")

                val platform = parent.platforms.firstOrNull {
                    parent.collider.collide(
                        entity.get(Player::class).base,
                        position.transformation,
                        it.get(BoundingBox::class),
                        it.get(Position::class).transformation
                    )
                }
                if (platform != null) {
                    // collide
                    // TODO: correct the position
                    return Idle(parent)
                }
            }

            parent.move(entity, delta)
            position.addGlobalTranslation(y = velocity, delta = delta)
            velocity -= gravity * delta
            return null
        }

        companion object {

            const val JUMP_HEIGHT = 2.5f // World unit
            const val JUMP_DURATION = 0.2f // Duration of the jump
        }
    }

    private fun move(entity: Entity, delta: Seconds): Boolean {
        val position = entity.position
        return if (input.isKeyPressed(Key.ARROW_LEFT)) {
            // still in the screen limit
            if (position.translation.x - (5f * delta) > -6.5f) {
                position.addGlobalTranslation(x = -5f, delta = delta)
            }
            true
        } else if (input.isKeyPressed(Key.ARROW_RIGHT)) {
            // still in the screen limit
            if (position.translation.x + (5f * delta) < 6.5f) {
                position.addGlobalTranslation(x = 5f, delta = delta)
            }
            true
        } else {
            false
        }
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
                val player = entityFactory.engine.createSprite(spr.sprites.values.first(), spr)
                val translation = it.transformation.toMat4().translation
                player.position.setGlobalTranslation(translation.x, translation.y, 1f)
                // bounding box
                lateinit var base: BoundingBox
                it.children.forEach { child ->
                    val component = BoundingBox.from(child.transformation.toMat4())
                    player.add(component)
                    // FIXME: hack to find the right bouding box. add a name to it?
                    if(child.name == "base") {
                        base = component
                    }
                }
                player.add(Player(base))
            } else if (it.name.startsWith("coin")) {
                val player = entityFactory.engine.createSprite(spr.sprites.values.first(), spr)
                player.get(SpriteComponent::class).switchToAnimation("coin")
                val translation = it.transformation.toMat4().translation
                player.position.setGlobalTranslation(translation.x, translation.y, 1f)
                player.add(Coin())
                // bounding box
                it.children.firstOrNull()?.run {
                    player.add(BoundingBox.from(this.transformation.toMat4()))
                }
            } else if (it.name.startsWith("platform")) {
                val platform = entityFactory.createFromNode(it, scene)
                platform.add(Platform())
            } else {
                entityFactory.createFromNode(it, scene)
            }
        }
    }

    override fun createSystems(engine: Engine): List<System> {
        return super.createSystems(engine) + listOf(CoinSystem(), PlayerSystem())
    }
}

// TODO: put that in the EntityFactory
fun Engine.createSprite(sprite: Sprite, scene: Scene): Entity = create {
    add(Position())
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
                        com.dwursteisen.minigdx.scene.api.model.Position(-0.5f, -0.5f, 0f),
                        Normal(0f, 0f, 0f),
                        uv = UV(0f, 0f)
                    ),
                    Vertex(
                        com.dwursteisen.minigdx.scene.api.model.Position(0.5f, -0.5f, 0f),
                        Normal(0f, 0f, 0f),
                        uv = UV(0f, 0f)
                    ),
                    Vertex(
                        com.dwursteisen.minigdx.scene.api.model.Position(-0.5f, 0.5f, 0f),
                        Normal(0f, 0f, 0f),
                        uv = UV(0f, 0f)
                    ),
                    Vertex(
                        com.dwursteisen.minigdx.scene.api.model.Position(0.5f, 0.5f, 0f),
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

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

class Player : StateMachineComponent()
class Coin : Component

class CoinSystem : System(EntityQuery(Coin::class)) {

    private val collider = AABBCollisionResolver()

    private val player by interested(EntityQuery(Player::class))

    override fun update(delta: Seconds, entity: Entity) {
        entity.get(Position::class).addLocalRotation(y = 90f, delta = delta)
        if(entity.hasComponent(BoundingBox::class) && collider.collide(
                entity,
                entity.position.globalTransformation,
                player.first()
            )) {
            // TODO: play a sound?
            entity.destroy()
        }
    }
}

class PlayerSystem : StateMachineSystem(Player::class) {

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
            if (parent.input.isKeyPressed(Key.ARROW_LEFT)) {
                entity.get(Position::class).addGlobalTranslation(x = -5f, delta = delta)
                return null
            } else if (parent.input.isKeyPressed(Key.ARROW_RIGHT)) {
                entity.get(Position::class).addGlobalTranslation(x = 5f, delta = delta)
                return null
            }

            return Idle(parent)
        }
    }

    class Jump(val parent: PlayerSystem) : State() {

        private val gravity = JUMP_HEIGHT / (JUMP_DURATION * JUMP_DURATION)
        private var velocity = sqrt(gravity * JUMP_HEIGHT)
        private var y = 0F

        override fun onEnter(entity: Entity) {
            entity.get(SpriteComponent::class).switchToAnimation("jump_up")
            y = entity.position.translation.y
        }

        override fun update(delta: Seconds, entity: Entity): State? {
            if (velocity < 0f) {
                entity.get(SpriteComponent::class).switchToAnimation("jump_down")
            }
            // FIXME: hack. Should use colllider instead
            if (entity.position.translation.y < y) {
                entity.position.setGlobalTranslation(y = y)
                return Idle(parent)
            }
            if (parent.input.isKeyPressed(Key.ARROW_LEFT)) {
                entity.get(Position::class).addGlobalTranslation(x = -5f, delta = delta)
            } else if (parent.input.isKeyPressed(Key.ARROW_RIGHT)) {
                entity.get(Position::class).addGlobalTranslation(x = 5f, delta = delta)
            }
            entity.get(Position::class).addGlobalTranslation(y = velocity, delta = delta)
            velocity -= gravity * delta
            return null
        }

        companion object {

            const val JUMP_HEIGHT = 2.5f // World unit
            const val JUMP_DURATION = 0.2f // Duration of the jump
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
                player.add(Player())
                // bounding box
                it.children.firstOrNull()?.run {
                    player.add(BoundingBox.from(this.transformation.toMat4()))
                }
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
            } else {
                entityFactory.createFromNode(it, scene)
            }
        }
    }

    override fun createSystems(engine: Engine): List<System> {
        return super.createSystems(engine) + listOf(CoinSystem(), PlayerSystem())
    }
}

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

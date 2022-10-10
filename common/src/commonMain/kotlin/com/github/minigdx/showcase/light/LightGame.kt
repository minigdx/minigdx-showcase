package com.github.minigdx.showcase.light

import com.dwursteisen.minigdx.scene.api.relation.ObjectType
import com.github.dwursteisen.minigdx.GameContext
import com.github.dwursteisen.minigdx.Seconds
import com.github.dwursteisen.minigdx.ecs.Engine
import com.github.dwursteisen.minigdx.ecs.components.LightComponent
import com.github.dwursteisen.minigdx.ecs.components.ModelComponent
import com.github.dwursteisen.minigdx.ecs.entities.Entity
import com.github.dwursteisen.minigdx.ecs.entities.EntityFactory
import com.github.dwursteisen.minigdx.ecs.entities.position
import com.github.dwursteisen.minigdx.ecs.systems.EntityQuery
import com.github.dwursteisen.minigdx.ecs.systems.System
import com.github.dwursteisen.minigdx.file.get
import com.github.dwursteisen.minigdx.game.Game
import com.github.dwursteisen.minigdx.graph.GraphNode
import com.github.dwursteisen.minigdx.graph.GraphScene
import com.github.minigdx.imgui.ImGui
import kotlin.random.Random

class CubeRotate : System(EntityQuery.of(ModelComponent::class)) {

    override fun update(delta: Seconds, entity: Entity) {
        entity.position.addLocalRotation(y = 90, delta = delta)
    }
}

class LightManagement(private val lightReference: GraphNode) : System(EntityQuery.Companion.of(LightComponent::class)) {

    override fun update(delta: Seconds) {
        with(ImGui) {
            container("Light") {
                if (entities.size < 5) {
                    if (button("Add a light")) {
                        val e = entityFactory.createFromNode(lightReference)
                        e.position.setGlobalTranslation(
                            x = (Random.nextFloat() * 6f - 3f),
                            z = (Random.nextFloat() * 3f), // only in front of the cube
                            y = 0f
                        )
                        val color = e.get(LightComponent::class).color
                        color.red = Random.nextFloat()
                        color.green = Random.nextFloat()
                        color.blue = Random.nextFloat()
                    }
                } else {
                    label("Too many lights!")
                    label("Please suppress one first")
                }
            }

            entities.forEach {
                container(it.name) {
                    if (button("Suppress")) {
                        it.destroy()
                    }
                }
            }

        }
    }

    override fun update(delta: Seconds, entity: Entity) = Unit
}

class LightGame(override val gameContext: GameContext) : Game {

    private val scene by gameContext.fileHandler.get<GraphScene>("light.protobuf")

    override fun createEntities(entityFactory: EntityFactory) {
        scene.nodes.forEach {
            entityFactory.createFromNode(it)
        }
    }

    override fun createSystems(engine: Engine): List<System> {
        return listOf(LightManagement(scene.getAll(ObjectType.LIGHT).first()), CubeRotate())
    }
}

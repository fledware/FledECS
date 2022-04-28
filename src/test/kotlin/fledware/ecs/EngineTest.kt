package fledware.ecs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals


internal class EngineTest {
  @Test
  fun testEmptyEngineUpdate() {
    val engine = createTestEngine()
    engine.update(1f)
  }

  @Test
  fun testWorldCreation() {
    val engine = createTestEngine()
    assertEquals(0, engine.data.worlds.size)
    engine.requestCreateWorld("testing", null, WorldBuilder::worldBuilderMovementOnly)
    assertEquals(0, engine.data.worlds.size)
    engine.handleRequests()
    assertEquals(1, engine.data.worlds.size)

    val world = engine.data.worlds["testing"]!!
    engine.update(1f)
    assertEquals(3, world.data.entities.size())
    assertEquals(2, world.data.systems.size)
    assertEquals(2, world.data.systems[MovementSystem::class].entities.size)
  }

  @Test
  fun testEntityUpdate() {
    val engine = createTestEngine()
    val world = engine.createTestWorld()
    engine.update(1f)
    val entities = world.data.systems[MovementSystem::class].entities
    val entity = entities.entities.find { it.name == "target" }!!

    assertEquals(Placement(1, 1, 1), entity.get())
    engine.update(1f)
    assertEquals(Placement(1, 1, 1), entity.get())

    entity.get<Movement>().deltaX = 1
    engine.update(1f)
    assertEquals(Placement(2, 1, 1), entity.get())
  }

  @Test
  fun testEntityUpdateForMultipleEntities() {
    val engine = createTestEngine()
    val world = engine.createTestWorld()
    engine.update(1f)
    val entities = world.data.systems[MovementSystem::class].entities.entities
    assertEquals(2, entities.size)
    val entity0 = entities.find { it.name == "target" }!!
    val entity1 = entities.find { it.name != "target" }!!

    assertEquals(Placement(1, 1, 1), entity0.get())
    assertEquals(Placement(8, 8, 1), entity1.get())
    entity0.get<Movement>().deltaX = -3
    entity1.get<Movement>().deltaX = -3

    engine.update(1f)
    assertEquals(Placement(0, 1, 1), entity0.get())
    assertEquals(Placement(5, 8, 1), entity1.get())
  }

  @Test
  fun testMultipleWorldsUpdate() {
    val engine = createTestEngine()
    val world1 = engine.createTestWorld("world1")
    val world2 = engine.createTestWorld("world2")
    val entity1 = world1.data.systems[MovementSystem::class].entities.entities.find { it.name == "target" }!!
    val entity2 = world2.data.systems[MovementSystem::class].entities.entities.find { it.name == "target" }!!
    assertNotEquals(entity1.id, entity2.id)

    assertEquals(Placement(1, 1, 1), entity1.get())
    assertEquals(Placement(1, 1, 1), entity2.get())
    engine.update(1f)
    assertEquals(Placement(1, 1, 1), entity1.get())
    assertEquals(Placement(1, 1, 1), entity2.get())

    entity1.get<Movement>().deltaX = 3
    entity2.get<Movement>().deltaX = 2
    engine.update(1f)
    assertEquals(Placement(4, 1, 1), entity1.get())
    assertEquals(Placement(3, 1, 1), entity2.get())
  }
}
package fledware.ecs

import kotlin.test.Test
import kotlin.test.assertEquals

internal class EntityGroupTest {
  @Test
  fun testEmptyEngineUpdate() {
    val engine = createTestEngine()
    val world = engine.createTestWorld()
    engine.update(1f)

    val entities = world.data.systems[MovementSystem::class].entities.entities
    assertEquals(2, entities.size)

    val entity = world.data.createEntity {
      add(Placement(4, 4, 2))
    }

    assertEquals(2, entities.size)
    entity.add(Movement(0, 0))
    engine.update(1f)
    assertEquals(3, entities.size)
  }
}
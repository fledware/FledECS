package fledware.ecs.ex

import fledware.ecs.createPersonEntity
import fledware.ecs.createTestEngine
import fledware.ecs.createTestWorld
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class EntityCachingTest {
  @Test
  fun testHappyPath() {
    val engine = createTestEngine().withEntityCaching()
    val world = engine.createTestWorld()

    val person = world.data.createPersonEntity(8, 8)
    assertEquals(world.name, person.worldSafe)
    assertTrue(world.data.entities.containsKey(person.id))

    val personId = person.id
    world.data.removeEntityToCache(person)
    val personOther = world.data.createPersonEntity(2, 2)

    assertEquals(personId, personOther.id)
  }

  @Test
  fun cachingApiWorksWithoutConfigured() {
    val engine = createTestEngine()
    val world = engine.createTestWorld()

    val person = world.data.createPersonEntity(8, 8)
    assertEquals(world.name, person.worldSafe)
    assertTrue(world.data.entities.containsKey(person.id))

    val personId = person.id
    world.data.removeEntityToCache(person)
    val personOther = world.data.createPersonEntity(2, 2)

    assertNotEquals(personId, personOther.id)
  }
}
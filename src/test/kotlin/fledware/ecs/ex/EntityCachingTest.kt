package fledware.ecs.ex

import fledware.ecs.Placement
import fledware.ecs.createPersonEntity
import fledware.ecs.createTestEngine
import fledware.ecs.createTestWorld
import fledware.ecs.debugToString
import fledware.ecs.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    assertEquals(0, engine.data.caching.getBucket("person").size)
    world.data.removeEntityToCache(person)
    assertEquals(1, engine.data.caching.getBucket("person").size)
    val personOther = world.data.createPersonEntity(2, 2)
    assertEquals(0, engine.data.caching.getBucket("person").size)

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

  @Test
  fun cachingResetsComponents() {
    val engine = createTestEngine().withEntityCaching().withEntityFlags()
    val world = engine.createTestWorld()
    val testFlag = engine.data.flagIndexOf("test")

    val person = world.data.createPersonEntity(8, 8)
    assertEquals(Placement(8, 8, 1), person.get())
    assertFalse(testFlag in person)

    assertEquals(world.name, person.worldSafe)
    assertTrue(world.data.entities.containsKey(person.id))
    val personId = person.id
    person += testFlag
    assertTrue(testFlag in person)
    engine.update(1f)

    world.data.removeEntityToCache(person)
    engine.update(1f)
    val person2 = world.data.createPersonEntity(7, 7)
    assertEquals(Placement(7, 7, 1), person.get())
    assertFalse(testFlag in person)
    assertEquals(personId, person2.id)

  }
}
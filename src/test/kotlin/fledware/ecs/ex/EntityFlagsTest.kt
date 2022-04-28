package fledware.ecs.ex

import fledware.ecs.createTestEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EntityFlagsTest {
  @Test
  fun testHappyPath() {
    val engine = createTestEngine().withEntityFlags()
    val entity = engine.data.createEntity {  }
    val flagOne = engine.data.flagIndexOf("FlagOne")

    assertFalse(flagOne in entity)
    entity += flagOne
    assertTrue(flagOne in entity)
    entity += flagOne
    assertTrue(flagOne in entity)
    entity -= flagOne
    assertFalse(flagOne in entity)
    entity -= flagOne
    assertFalse(flagOne in entity)
  }

  @Test
  fun testClear() {
    val engine = createTestEngine().withEntityFlags()
    val entity = engine.data.createEntity {  }
    val flagOne = engine.data.flagIndexOf("FlagOne")
    entity += flagOne
    assertTrue(flagOne in entity)
    entity.flagClearAll()
    assertFalse(flagOne in entity)
  }

  @Test
  fun testLostOfFlagsAreSeparate() {
    val engine = createTestEngine().withEntityFlags()
    val entity = engine.data.createEntity {  }
    val flags = (0..1000).map { engine.data.flagIndexOf("SomeFlag$it") }
    flags.forEach { testing ->
      entity += testing
      // test that only the added flag is set
      flags.forEach { other ->
        assertEquals(other == testing, other in entity)
      }
      entity -= testing
    }
  }

  @Test
  fun testNotifyUpdateCorrectForSet() {
    val engine = createTestEngine().withEntityFlags()
    val entity = engine.data.createEntity {  }
    val events = mutableListOf<Any?>()
    entity.components.onUpdate = { events += it }
    entity += engine.data.flagIndexOf("FlagYo")
    // adding the component causes a notification and also the flag
    assertEquals(2, events.size)
    entity += engine.data.flagIndexOf("FlagYo")
    assertEquals(2, events.size)
    entity += engine.data.flagIndexOf("FlagYo2")
    assertEquals(3, events.size)
    entity += engine.data.flagIndexOf("FlagYo2")
    assertEquals(3, events.size)
  }

  @Test
  fun testNotifyUpdateCorrectForUnset() {
    val engine = createTestEngine().withEntityFlags()
    val entity = engine.data.createEntity {  }
    val events = mutableListOf<Any?>()
    entity.components.onUpdate = { events += it }

    entity -= engine.data.flagIndexOf("FlagYo")
    assertEquals(0, events.size)
    entity += engine.data.flagIndexOf("FlagYo")
    assertEquals(2, events.size)

    entity -= engine.data.flagIndexOf("FlagYo2")
    assertEquals(2, events.size)
    entity += engine.data.flagIndexOf("FlagYo2")
    assertEquals(3, events.size)

    entity -= engine.data.flagIndexOf("FlagYo")
    assertEquals(4, events.size)
    entity -= engine.data.flagIndexOf("FlagYo")
    assertEquals(4, events.size)
    entity -= engine.data.flagIndexOf("FlagYo2")
    assertEquals(5, events.size)
    entity -= engine.data.flagIndexOf("FlagYo2")
    assertEquals(5, events.size)
  }
}
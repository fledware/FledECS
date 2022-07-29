package fledware.ecs.ex

import fledware.ecs.Engine
import fledware.ecs.Entity
import fledware.ecs.EntityEvents
import fledware.ecs.ManagedEntity
import fledware.ecs.createTestEngine
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EntityFlagsTest {
  companion object {
    @JvmStatic
    fun engineConfigurations(): Stream<Arguments> = Stream.of(
        Arguments.of("memory-index", { createTestEngine().withEntityFlags() }),
        Arguments.of("static-index", { createTestEngine().withEntityFlags().withStaticEngineContext() })
    )
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("engineConfigurations")
  fun testHappyPath(name: String, engineFactory: () -> Engine) {
    val engine = engineFactory()
    val entity = engine.data.createEntity { }
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

  @ParameterizedTest(name = "{0}")
  @MethodSource("engineConfigurations")
  fun testClear(name: String, engineFactory: () -> Engine) {
    val engine = engineFactory()
    val entity = engine.data.createEntity { }
    val flagOne = engine.data.flagIndexOf("FlagOne")
    entity += flagOne
    assertTrue(flagOne in entity)
    entity.flagClearAll()
    assertFalse(flagOne in entity)
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("engineConfigurations")
  fun testLostOfFlagsAreSeparate(name: String, engineFactory: () -> Engine) {
    val engine = engineFactory()
    val entity = engine.data.createEntity { }
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

  private fun Entity.entityNotification(block: () -> Unit) {
    (this as ManagedEntity).registerToWorld("test", object : EntityEvents {
      override fun onNameChange(entity: Entity) = TODO("Not yet implemented")
      override fun onUpdate(entity: Entity) = block()
    })
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("engineConfigurations")
  fun testNotifyUpdateCorrectForSet(name: String, engineFactory: () -> Engine) {
    val engine = engineFactory()
    val entity = engine.data.createEntity { }
    var eventCount = 0
    entity.entityNotification { eventCount++ }

    entity += engine.data.flagIndexOf("FlagYo")
    // adding the component causes a notification and also the flag
    assertEquals(2, eventCount)
    entity += engine.data.flagIndexOf("FlagYo")
    assertEquals(2, eventCount)
    entity += engine.data.flagIndexOf("FlagYo2")
    assertEquals(3, eventCount)
    entity += engine.data.flagIndexOf("FlagYo2")
    assertEquals(3, eventCount)
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("engineConfigurations")
  fun testNotifyUpdateCorrectForUnset(name: String, engineFactory: () -> Engine) {
    val engine = engineFactory()
    val entity = engine.data.createEntity { }
    var eventCount = 0
    entity.entityNotification { eventCount++ }

    entity -= engine.data.flagIndexOf("FlagYo")
    assertEquals(0, eventCount)
    entity += engine.data.flagIndexOf("FlagYo")
    assertEquals(2, eventCount)

    entity -= engine.data.flagIndexOf("FlagYo2")
    assertEquals(2, eventCount)
    entity += engine.data.flagIndexOf("FlagYo2")
    assertEquals(3, eventCount)

    entity -= engine.data.flagIndexOf("FlagYo")
    assertEquals(4, eventCount)
    entity -= engine.data.flagIndexOf("FlagYo")
    assertEquals(4, eventCount)
    entity -= engine.data.flagIndexOf("FlagYo2")
    assertEquals(5, eventCount)
    entity -= engine.data.flagIndexOf("FlagYo2")
    assertEquals(5, eventCount)
  }
}
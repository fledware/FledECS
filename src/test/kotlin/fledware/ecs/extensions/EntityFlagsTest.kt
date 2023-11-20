package fledware.ecs.extensions

import fledware.ecs.OnEntityUpdated
import fledware.ecs.World
import fledware.ecs.util.add
import fledware.ecs.world.DefaultWorld
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
        Arguments.of("memory-index", { DefaultWorld().withEntityFlags() }),
//        Arguments.of("static-index", { createTestEngine().withEntityFlags().withStaticEngineContext() }),
    )
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("engineConfigurations")
  fun testHappyPath(name: String, worldFactory: () -> World) {
    val world = worldFactory()
    val entity = world.entities.create { }
    val flagOne = world.flagIndexOf("FlagOne")

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
  fun testClear(name: String, worldFactory: () -> World) {
    val world = worldFactory()
    val entity = world.entities.create { }
    val flagOne = world.flagIndexOf("FlagOne")
    entity += flagOne
    assertTrue(flagOne in entity)
    entity.flagClearAll()
    assertFalse(flagOne in entity)
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("engineConfigurations")
  fun testLostOfFlagsAreSeparate(name: String, worldFactory: () -> World) {
    val world = worldFactory()
    val entity = world.entities.create { }
    val flags = (0..1000).map { world.flagIndexOf("SomeFlag$it") }
    flags.forEach { testing ->
      entity += testing
      // test that only the added flag is set
      flags.forEach { other ->
        assertEquals(other == testing, other in entity)
      }
      entity -= testing
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("engineConfigurations")
  fun testNotifyUpdateCorrectForSet(name: String, worldFactory: () -> World) {
    val world = worldFactory()
    val entity = world.entities.create { }
    var eventCount = 0
    world.updateEvents.add<OnEntityUpdated> { eventCount++ }

    entity += world.flagIndexOf("FlagYo")
    world.update(1f)
    assertEquals(1, eventCount)
    entity += world.flagIndexOf("FlagYo")
    world.update(1f)
    assertEquals(1, eventCount)
    entity += world.flagIndexOf("FlagYo2")
    world.update(1f)
    assertEquals(2, eventCount)
    entity += world.flagIndexOf("FlagYo2")
    world.update(1f)
    assertEquals(2, eventCount)
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("engineConfigurations")
  fun testNotifyUpdateCorrectForUnset(name: String, worldFactory: () -> World) {
    val world = worldFactory()
    val entity = world.entities.create { }
    var eventCount = 0
    world.updateEvents.add<OnEntityUpdated> { eventCount++ }

    entity -= world.flagIndexOf("FlagYo")
    world.update(1f)
    assertEquals(0, eventCount)
    entity += world.flagIndexOf("FlagYo")
    world.update(1f)
    assertEquals(1, eventCount)

    entity -= world.flagIndexOf("FlagYo2")
    world.update(1f)
    assertEquals(1, eventCount)
    entity += world.flagIndexOf("FlagYo2")
    world.update(1f)
    assertEquals(2, eventCount)

    entity -= world.flagIndexOf("FlagYo")
    world.update(1f)
    assertEquals(3, eventCount)
    entity -= world.flagIndexOf("FlagYo")
    world.update(1f)
    assertEquals(3, eventCount)
    entity -= world.flagIndexOf("FlagYo2")
    world.update(1f)
    assertEquals(4, eventCount)
    entity -= world.flagIndexOf("FlagYo2")
    world.update(1f)
    assertEquals(4, eventCount)
  }
}
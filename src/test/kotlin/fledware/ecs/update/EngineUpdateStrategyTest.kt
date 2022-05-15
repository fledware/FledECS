package fledware.ecs.update

import fledware.ecs.EngineUpdateStrategy
import fledware.ecs.Movement
import fledware.ecs.Placement
import fledware.ecs.createPersonEntity
import fledware.ecs.createWorldAndFlush
import fledware.ecs.ex.execute
import fledware.ecs.getOrNull
import fledware.ecs.impl.DefaultEngine
import fledware.ecs.impl.executorUpdateStrategy
import fledware.ecs.impl.mainThreadUpdateStrategy
import fledware.ecs.worldBuilderMovementOnly
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.stream.Stream
import kotlin.math.max
import kotlin.system.measureTimeMillis
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class EngineUpdateStrategyTest {
  companion object {
    @JvmStatic
    fun engineConfigurations(): Stream<Arguments> = Stream.of(
        Arguments.of("default", mainThreadUpdateStrategy()),
        Arguments.of("worker-reentrant-2", executorUpdateStrategy(2)),
        Arguments.of("worker-reentrant-4", executorUpdateStrategy(4)),
        Arguments.of("worker-reentrant-6", executorUpdateStrategy(6)),
        Arguments.of("worker-reentrant-8", executorUpdateStrategy(8)),
        Arguments.of("worker-reentrant-10", executorUpdateStrategy(10))
    )
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("engineConfigurations")
  @Timeout(2)
  fun simpleMovementOneWorld(name: String, strategy: EngineUpdateStrategy) {
    actualTest(strategy, 1, name)
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("engineConfigurations")
  @Timeout(2)
  fun simpleMovementLessThanProcessorCount(name: String, strategy: EngineUpdateStrategy) {
    val worlds = max(0, Runtime.getRuntime().availableProcessors() / 2)
    actualTest(strategy, worlds, name)
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("engineConfigurations")
  @Timeout(2)
  fun simpleMovementAtProcessorCount(name: String, strategy: EngineUpdateStrategy) {
    actualTest(strategy, Runtime.getRuntime().availableProcessors(), name)
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("engineConfigurations")
  @Timeout(5)
  fun simpleMovementLotsFromProcessorCount(name: String, strategy: EngineUpdateStrategy) {
    actualTest(strategy, Runtime.getRuntime().availableProcessors() * 2, name)
  }

  private fun actualTest(strategy: EngineUpdateStrategy,
                         worldCount: Int,
                         name: String) {
    val engine = DefaultEngine(strategy)
    try {
      measureTimeMillis {
        engine.start()
      }.also { println("$name start time: $it") }
      measureTimeMillis {
        repeat(worldCount) {
          engine.createWorldAndFlush("world-$it") {
            worldBuilderMovementOnly()
            repeat(10) {
              repeat(10) { x ->
                repeat(10) { y ->
                  createPersonEntity(x, y)
                }
              }
            }
          }
        }
      }.also { println("$name create time: $it") }
      measureTimeMillis {
        repeat(50) {
          engine.data.worlds.values.forEach { world ->
            world.data.entities.values().forEach { entity ->
              entity.getOrNull<Movement>()?.deltaY = -1
            }
          }
          engine.update(1f)
        }
      }.also { println("$name update time: $it") }
      engine.data.worlds.values.forEach { world ->
        world.data.entities.values().forEach { entity ->
          Assertions.assertEquals(0, entity.getOrNull<Placement>()?.y ?: 0)
        }
      }
    }
    finally {
      engine.shutdown()
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("engineConfigurations")
  @Timeout(2)
  fun worldOrderingTest(name: String, strategy: EngineUpdateStrategy) {
    val engine = DefaultEngine(strategy)
    engine.start()
    engine.updateStrategy.createWorldUpdateGroup("firsts", 1)
    engine.updateStrategy.createWorldUpdateGroup("others", 4)
    engine.updateStrategy.createWorldUpdateGroup("after", 3)
    engine.updateStrategy.createWorldUpdateGroup("haha", 5)
    engine.updateStrategy.createWorldUpdateGroup("okokokok", 10)

    fun createWorldFor(group: String, index: Int) {
      engine.createWorldAndFlush("$group-$index") {
        updateGroup = group
        worldBuilderMovementOnly()
      }
    }

    repeat(5) { createWorldFor("firsts", it) }
    createWorldFor("okokokok", 0)
    repeat(20) { createWorldFor("after", it) }
    repeat(8) { createWorldFor("others", it) }
    engine.update(1f)

    val updated = ConcurrentLinkedQueue<String>()
    engine.data.worlds.values.forEach { world ->
      world.execute { updated += world.name }
    }
    engine.update(1f)

    repeat(5) {
      val check = updated.remove()
      assertEquals("firsts", check.split('-')[0])
    }
    repeat(20) {
      val check = updated.remove()
      assertEquals("after", check.split('-')[0])
    }
    repeat(8) {
      val check = updated.remove()
      assertEquals("others", check.split('-')[0])
    }
    repeat(1) {
      val check = updated.remove()
      assertEquals("okokokok", check.split('-')[0])
    }

    assertTrue(updated.isEmpty())
  }
}
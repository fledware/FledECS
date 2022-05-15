package fledware.ecs.update

import fledware.ecs.EngineUpdateStrategy
import fledware.ecs.Movement
import fledware.ecs.Placement
import fledware.ecs.createPersonEntity
import fledware.ecs.createWorldAndFlush
import fledware.ecs.getOrNull
import fledware.ecs.impl.DefaultEngine
import fledware.ecs.worldBuilderMovementOnly
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.concurrent.Executors
import java.util.stream.Stream
import kotlin.math.max
import kotlin.system.measureTimeMillis

internal class EngineUpdateStrategyTest {
  companion object {
    @JvmStatic
    fun engineConfigurations(): Stream<Arguments> = Stream.of(
        Arguments.of("default", MainThreadUpdateStrategy()),
        Arguments.of("worker-reentrant-2", AtomicWorldUpdateStrategy(BurstCyclicalJobWorkerPool(2, CyclicalLatchReentrantLock()))),
        Arguments.of("worker-reentrant-4", AtomicWorldUpdateStrategy(BurstCyclicalJobWorkerPool(4, CyclicalLatchReentrantLock()))),
        Arguments.of("worker-reentrant-6", AtomicWorldUpdateStrategy(BurstCyclicalJobWorkerPool(6, CyclicalLatchReentrantLock()))),
        Arguments.of("worker-reentrant-8", AtomicWorldUpdateStrategy(BurstCyclicalJobWorkerPool(8, CyclicalLatchReentrantLock()))),
        Arguments.of("worker-reentrant-10", AtomicWorldUpdateStrategy(BurstCyclicalJobWorkerPool(10, CyclicalLatchReentrantLock()))),
        Arguments.of("worker-park-2", AtomicWorldUpdateStrategy(BurstCyclicalJobWorkerPool(2, CyclicalLatchParkLock()))),
        Arguments.of("worker-park-4", AtomicWorldUpdateStrategy(BurstCyclicalJobWorkerPool(4, CyclicalLatchParkLock()))),
        Arguments.of("worker-park-6", AtomicWorldUpdateStrategy(BurstCyclicalJobWorkerPool(6, CyclicalLatchParkLock()))),
        Arguments.of("worker-park-8", AtomicWorldUpdateStrategy(BurstCyclicalJobWorkerPool(8, CyclicalLatchParkLock()))),
        Arguments.of("worker-park-10", AtomicWorldUpdateStrategy(BurstCyclicalJobWorkerPool(10, CyclicalLatchParkLock()))),
        Arguments.of("executor-1", AtomicWorldUpdateStrategy(BurstCyclicalJobExecutorPool(Executors.newSingleThreadExecutor(), true))),
        Arguments.of("executor-2", AtomicWorldUpdateStrategy(BurstCyclicalJobExecutorPool(Executors.newFixedThreadPool(2), true))),
        Arguments.of("executor-4", AtomicWorldUpdateStrategy(BurstCyclicalJobExecutorPool(Executors.newFixedThreadPool(4), true))),
        Arguments.of("executor-6", AtomicWorldUpdateStrategy(BurstCyclicalJobExecutorPool(Executors.newFixedThreadPool(6), true))),
        Arguments.of("executor-8", AtomicWorldUpdateStrategy(BurstCyclicalJobExecutorPool(Executors.newFixedThreadPool(8), true))),
        Arguments.of("executor-10", AtomicWorldUpdateStrategy(BurstCyclicalJobExecutorPool(Executors.newFixedThreadPool(10), true))),
        Arguments.of("executor-steal", AtomicWorldUpdateStrategy(BurstCyclicalJobExecutorPool(Executors.newWorkStealingPool(), true))),
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

}
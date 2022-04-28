package fledware.ecs

import fledware.ecs.threads.CyclicalLatchParkLock
import fledware.ecs.threads.CyclicalLatchReentrantLock
import fledware.ecs.threads.BurstCyclicalJobExecutorPool
import fledware.ecs.threads.BurstCyclicalJobWorkerPool
import fledware.ecs.update.AtomicWorldUpdateStrategy
import fledware.ecs.update.DefaultUpdateStrategy
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.concurrent.Executors
import java.util.stream.Stream
import kotlin.system.measureTimeMillis

internal class EngineUpdateStrategyTest {
  companion object {
    @JvmStatic
    fun engineConfigurations(): Stream<Arguments> = Stream.of(
        Arguments.of("default", DefaultUpdateStrategy()),
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
  fun simpleMovementSmall(name: String, strategy: EngineUpdateStrategy) {
    actualTest(strategy, 10, name)
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("engineConfigurations")
  @Timeout(5)
  fun simpleMovementBig(name: String, strategy: EngineUpdateStrategy) {
    actualTest(strategy, 50, name)
  }

  private fun actualTest(strategy: EngineUpdateStrategy,
                         worldCount: Int,
                         name: String) {
    val engine = DefaultEngine(strategy, ConcurrentEngineData())
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
        repeat(500) {
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
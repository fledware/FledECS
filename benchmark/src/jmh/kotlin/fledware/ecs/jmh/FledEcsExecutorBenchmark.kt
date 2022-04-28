package fledware.ecs.jmh

import fledware.ecs.ConcurrentEngineData
import fledware.ecs.DefaultEngine
import fledware.ecs.Engine
import fledware.ecs.benchmark.Constants
import fledware.ecs.benchmark.FledCollisionSystem
import fledware.ecs.benchmark.FledMovementSystem
import fledware.ecs.benchmark.FledRandomAdder
import fledware.ecs.benchmark.FledRandomDeleter
import fledware.ecs.benchmark.FledRemovalSystem
import fledware.ecs.benchmark.FledStateSystem
import fledware.ecs.benchmark.stdWorldEntity
import fledware.ecs.createWorldAndFlush
import fledware.ecs.threads.BurstCyclicalJobExecutorPool
import fledware.ecs.update.AtomicWorldUpdateStrategy
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.TearDown
import java.util.concurrent.Executors
import kotlin.math.max


open class FledEcsExecutorBenchmark : AbstractBenchmark() {
  private lateinit var engine: Engine

  @Setup
  open fun init() {
    val pool = BurstCyclicalJobExecutorPool(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()), true)
    engine = DefaultEngine(AtomicWorldUpdateStrategy(pool), ConcurrentEngineData())
    val worldCount = max(Runtime.getRuntime().availableProcessors(), entityCount / 1024)
    val worldEntityCount = entityCount / worldCount
    (0 until worldCount).forEach { worldIndex ->
      engine.createWorldAndFlush("world$worldIndex") {
        addSystem(FledMovementSystem())
        addSystem(FledStateSystem())
        addSystem(FledCollisionSystem())
        addSystem(FledRemovalSystem())
        addSystem(FledRandomDeleter(worldIndex))
        addSystem(FledRandomAdder(worldIndex))
        (0 until worldEntityCount).forEach { entityIndex ->
          createEntity {
            stdWorldEntity(entityIndex)
          }
        }
      }
    }
  }

  @TearDown
  fun shutdown() {
    engine.shutdown()
  }

  @Benchmark
  open fun baseline() {
    engine.update(Constants.DELTA_TIME)
  }
}

package fledware.ecs.jmh

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
import fledware.ecs.impl.DefaultEngine
import fledware.ecs.update.AtomicWorldUpdateStrategy
import fledware.ecs.update.BurstCyclicalJobWorkerPool
import fledware.ecs.update.CyclicalLatchParkLock
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import org.openjdk.jmh.annotations.Timeout
import java.util.concurrent.TimeUnit
import kotlin.math.max

@State(Scope.Benchmark)
open class FledEcsThreadedBenchmark : AbstractBenchmark() {
  private lateinit var engine: Engine

  @Setup
  open fun init() {
    val pool = BurstCyclicalJobWorkerPool(Runtime.getRuntime().availableProcessors(), CyclicalLatchParkLock())
    engine = DefaultEngine(AtomicWorldUpdateStrategy(pool))
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
    engine.start()
  }

  @TearDown
  fun shutdown() {
    engine.shutdown()
  }

  @Benchmark
  @Timeout(time = 1, timeUnit = TimeUnit.SECONDS)
  open fun baseline() {
    engine.update(Constants.DELTA_TIME)
  }
}

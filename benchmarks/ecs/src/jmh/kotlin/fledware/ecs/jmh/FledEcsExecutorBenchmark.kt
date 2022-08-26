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
import fledware.ecs.impl.executorUpdateStrategy
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import kotlin.math.max

@State(Scope.Benchmark)
open class FledEcsExecutorBenchmark : AbstractBenchmark() {
  private lateinit var engine: Engine

  @Setup
  open fun init() {
    engine = DefaultEngine(executorUpdateStrategy())
    engine.start()
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

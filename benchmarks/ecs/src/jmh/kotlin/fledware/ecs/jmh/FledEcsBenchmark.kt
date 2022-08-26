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
import fledware.ecs.impl.mainThreadUpdateStrategy
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown

@State(Scope.Benchmark)
open class FledEcsBenchmark : AbstractBenchmark() {
  private lateinit var engine: Engine

  @Setup
  open fun init() {
    engine = DefaultEngine(mainThreadUpdateStrategy())
    engine.start()
    val world = engine.createWorldAndFlush("main") {
      addSystem(FledMovementSystem())
      addSystem(FledStateSystem())
      addSystem(FledCollisionSystem())
      addSystem(FledRemovalSystem())
      addSystem(FledRandomDeleter())
      addSystem(FledRandomAdder())
    }
    for (entityIndex in 0 until entityCount) {
      world.data.createEntity {
        stdWorldEntity(entityIndex)
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

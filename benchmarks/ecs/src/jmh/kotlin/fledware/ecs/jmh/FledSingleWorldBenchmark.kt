package fledware.ecs.jmh

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import fledware.ecs.World
import fledware.ecs.benchmark.AshleyCollisionSystem
import fledware.ecs.benchmark.AshleyMovementSystem
import fledware.ecs.benchmark.AshleyRandomAdder
import fledware.ecs.benchmark.AshleyRandomDeleter
import fledware.ecs.benchmark.AshleyRemovalSystem
import fledware.ecs.benchmark.AshleyStateSystem
import fledware.ecs.benchmark.Constants
import fledware.ecs.benchmark.FledCollisionSystem
import fledware.ecs.benchmark.FledMovementSystem
import fledware.ecs.benchmark.FledRandomAdder
import fledware.ecs.benchmark.FledRandomDeleter
import fledware.ecs.benchmark.FledRemovalSystem
import fledware.ecs.benchmark.FledStateSystem
import fledware.ecs.benchmark.stdWorldEntity
import fledware.ecs.world.DefaultWorld
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown

@State(Scope.Benchmark)
open class FledSingleWorldBenchmark : AbstractBenchmark() {
  private lateinit var world: World

  @Setup
  fun init() {
    world = DefaultWorld()
    world.systems.add(FledMovementSystem())
    world.systems.add(FledStateSystem())
    world.systems.add(FledCollisionSystem())
    world.systems.add(FledRemovalSystem())
    world.systems.add(FledRandomDeleter())
    world.systems.add(FledRandomAdder())
    for (entityIndex in 0 until entityCount) {
      world.entities.create {
        stdWorldEntity(entityIndex)
      }
    }
  }

  @TearDown
  fun shutdown() {
    world.destroy()
  }

  @Benchmark
  fun baseline() {
    world.update(Constants.DELTA_TIME)
  }
}
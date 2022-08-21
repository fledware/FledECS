package fledware.ecs.jmh

import fledware.ecs.Engine
import fledware.ecs.ex.withEntityFlags
import fledware.ecs.impl.DefaultEngine
import fledware.ecs.impl.executorUpdateStrategy
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State

@State(Scope.Benchmark)
open class ValueMemoryBenchmark : ValueBenchmark() {
  override fun createEngine(): Engine {
    return DefaultEngine(executorUpdateStrategy())
        .withEntityFlags()
  }
}
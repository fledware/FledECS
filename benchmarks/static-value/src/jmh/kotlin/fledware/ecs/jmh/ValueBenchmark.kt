package fledware.ecs.jmh

import fledware.ecs.Engine
import fledware.ecs.Entity
import fledware.ecs.World
import fledware.ecs.createWorldAndFlush
import fledware.ecs.ex.contains
import fledware.ecs.ex.FlagIndex
import fledware.ecs.ex.flagIndexOf
import fledware.ecs.ex.minusAssign
import fledware.ecs.ex.plusAssign
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import org.openjdk.jmh.annotations.Threads
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@Threads(1)
@Fork(1)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
abstract class ValueBenchmark {
  private lateinit var engine: Engine
  private lateinit var world: World
  private lateinit var entity: Entity
  private lateinit var flag: FlagIndex

  protected abstract fun createEngine(): Engine

  @Setup
  open fun init() {
    engine = createEngine()
    engine.start()
    world = engine.createWorldAndFlush("test") {
      entity = createEntity {  }
    }
    engine.update(0f)
    flag = engine.data.flagIndexOf("testing")
  }

  @TearDown
  fun shutdown() {
    engine.shutdown()
  }

  @Benchmark
  open fun baseline() {
    assert(flag !in entity)
    entity += flag
    assert(flag in entity)
    entity -= flag
    assert(flag !in entity)
  }
}
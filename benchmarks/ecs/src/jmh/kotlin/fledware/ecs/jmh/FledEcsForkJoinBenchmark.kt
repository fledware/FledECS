//package fledware.ecs.jmh
//
//import fledware.ecs.Engine
//import fledware.ecs.impl.DefaultEngine
//import fledware.ecs.impl.forkJoinPoolUpdateStrategy
//import org.openjdk.jmh.annotations.Scope
//import org.openjdk.jmh.annotations.State
//
//@State(Scope.Benchmark)
//open class FledEcsForkJoinBenchmark : AbstractFledBenchmark() {
//  override fun createEngine(): Engine {
//    return DefaultEngine(forkJoinPoolUpdateStrategy())
//  }
//}

//package fledware.ecs.jmh
//
//import fledware.ecs.Engine
//import fledware.ecs.impl.DefaultEngine
//import fledware.ecs.impl.mainThreadUpdateStrategy
//import org.openjdk.jmh.annotations.Scope
//import org.openjdk.jmh.annotations.State
//
//@State(Scope.Benchmark)
//open class FledEcsBenchmark : AbstractFledBenchmark() {
//  override fun createEngine(): Engine {
//    return DefaultEngine(mainThreadUpdateStrategy())
//  }
//}

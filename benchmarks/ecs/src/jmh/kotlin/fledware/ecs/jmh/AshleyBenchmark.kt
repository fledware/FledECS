//package fledware.ecs.jmh
//
//import com.badlogic.ashley.core.Engine
//import com.badlogic.ashley.core.Entity
//import AshleyCollisionSystem
//import AshleyMovementSystem
//import AshleyRandomAdder
//import AshleyRandomDeleter
//import AshleyRemovalSystem
//import AshleyStateSystem
//import Constants
//import stdWorldEntity
//import org.openjdk.jmh.annotations.Benchmark
//import org.openjdk.jmh.annotations.Scope
//
//import org.openjdk.jmh.annotations.Setup
//import org.openjdk.jmh.annotations.State
//
//@State(Scope.Benchmark)
//open class AshleyBenchmark : AbstractBenchmark() {
//  private lateinit var engine: Engine
//
//  @Setup
//  open fun init() {
//    engine = Engine()
//    engine.addSystem(AshleyMovementSystem())
//    engine.addSystem(AshleyStateSystem())
//    engine.addSystem(AshleyCollisionSystem())
//    engine.addSystem(AshleyRemovalSystem())
//    engine.addSystem(AshleyRandomDeleter())
//    engine.addSystem(AshleyRandomAdder())
//    for (entityIndex in 0 until entityCount) {
//      engine.addEntity(Entity().apply { stdWorldEntity(entityIndex) })
//    }
//  }
//
//  @Benchmark
//  open fun baseline() {
//    engine.update(Constants.DELTA_TIME)
//  }
//}

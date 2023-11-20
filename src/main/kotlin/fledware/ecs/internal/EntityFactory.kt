package fledware.ecs.internal

import fledware.ecs.Entity
import fledware.ecs.OnCreateEntityWorldHook
import fledware.ecs.World
import fledware.ecs.util.Mapper
import java.util.concurrent.atomic.AtomicLong

interface EntityFactory: OnCreateEntityWorldHook {
  val mapper: Mapper<Any>

  fun nextId(): Long

  fun factory(): ManagedEntity = ManagedEntity(nextId().toString(), mapper.list())
}

class DefaultEntityFactory(
    override val mapper: Mapper<Any> = Mapper(),
    startingId: Long = 0
) : EntityFactory {
  override val order: Int = Int.MAX_VALUE
  private val ids = AtomicLong(startingId)
  override fun nextId(): Long = ids.incrementAndGet()
  override fun invoke(world: World): Entity = factory()
}

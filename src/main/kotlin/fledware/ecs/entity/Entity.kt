package fledware.ecs.entity

import fledware.ecs.util.Mapper
import kotlin.reflect.KClass

@JvmInline
value class Entity(val id: Int) {
  companion object {
    const val IndexIdMask = 0x00FFFFFF
    const val TypeIdMaskUnshifted = 0x0000007F
    const val TypeIdMaskShifted = 0x7F000000
    const val TypeIdShift = 24

    fun of(typeId: Int, indexId: Int): Entity {
      if (typeId.and(TypeIdMaskUnshifted) != typeId)
        throw IllegalArgumentException("invalid type")
      if (indexId.and(IndexIdMask) != indexId)
        throw IllegalArgumentException("invalid index")
      return Entity(typeId.shl(TypeIdShift).or(indexId))
    }
  }

  val typeId: Int
    get() = id.and(TypeIdMaskShifted).shr(TypeIdShift)
  val indexId: Int
    get() = id.and(IndexIdMask)
}

class EntityComponentsManager {
  val mapper = Mapper<KClass<*>>()
  val componentLists = mapper.list<Array<Any>>()

  fun <T : Any> getComponentList(componentClass: KClass<T>): Array<T> {
    val componentList = componentLists.getByKey(componentClass)
    @Suppress("UNCHECKED_CAST")
    return componentList as Array<T>
  }
}

class EntityComponents

inline fun <reified T: Any> EntityComponentsManager.getComponentList() =
    this.getComponentList(T::class)

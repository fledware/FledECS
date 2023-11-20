package fledware.ecs

/**
 * A central interface to manage and share [EntityGroup]s
 */
interface EntityGroupManager {
  /**
   * the amount of managed groups
   */
  val size: Int
  /**
   * the world this manager is for
   */
  val world: World
  /**
   * gets an already created [EntityGroup] by its name.
   */
  operator fun get(name: String): EntityGroup
  /**
   * creates an [EntityGroup] to include the given entities.
   *
   * This will cause an iteration over all entities to check which
   * entities will initially be in this group.
   *
   * If no name is passed, a random name is generated.
   */
  fun create(name: String = "", include: (entity: Entity) -> Boolean): EntityGroup
  /**
   * removes an [EntityGroup] by its name.
   */
  fun remove(name: String)
  /**
   * removes the given [EntityGroup].
   */
  fun remove(group: EntityGroup) = remove(group.name)
}

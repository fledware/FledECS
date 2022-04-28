package fledware.ecs

/**
 * The public interface for the engine and other worlds to work with.
 * Depending on the implementation, all the methods should
 * be thread safe.
 */
interface World {
  /**
   * the engine this world belongs to
   */
  val engine: Engine

  /**
   * the unique name of this world within the engine
   */
  val name: String

  /**
   * the private data of this world. The default implementation
   * of World will throw an exception if access is attempted
   * during an update.
   */
  val data: WorldData

  /**
   * the events for this world.
   */
  val events: WorldEvents

  /**
   * the options used to create this world. Can be any user
   * defined object to help drive what this World does or how it's
   * created.
   */
  val options: Any?

  /**
   * a count of how many times this world has been updated
   */
  val updateIndex: Long

  /**
   * send a message to this world in a thread safe way. The default
   * implementation buffers messages and handles them in thread safe
   * ways.
   */
  fun receiveMessage(message: Any)

  /**
   * sends an entity to this World in a thread safe way. The default
   * implementation buffers these and does the actual import later.
   */
  fun receiveEntity(entity: Entity)
}

interface WorldManaged : World {
  /**
   * Gets the [WorldData] of this world, but will never throw an exception.
   *
   * Don't use this unless you fully understand the effects it will cause
   * on threading.
   */
  val dataSafe: WorldData
  /**
   * used by the engine to let the world know it is wired up externally
   * and can start initialization.
   */
  fun onCreate()

  /**
   * used by the engine to signal this world is to never be updated
   * again and should be cleaned up and deleted.
   */
  fun onDestroy()

  /**
   * called on every world before any update methods are called.
   */
  fun preUpdate()

  /**
   * called on every world after preUpdate(), and before postUpdate()
   */
  fun update(delta: Float)

  /**
   * called on every world after all worlds returned from update()
   */
  fun postUpdate()
}

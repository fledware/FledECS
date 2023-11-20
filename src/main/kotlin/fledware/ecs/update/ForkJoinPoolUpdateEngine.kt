package fledware.ecs.update

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinTask
import java.util.concurrent.RecursiveAction
import java.util.concurrent.TimeUnit

class ForkJoinPoolUpdateEngine(
    val pool: ForkJoinPool = ForkJoinPool(),
    private val ownsPool: Boolean = true
) : UpdateEngine {

  override val groups = ConcurrentHashMap<Any, UpdateGroup>()

  private var orderedGroupsCache: List<ForkJoinPoolUpdateGroup>? = null

  val lock = SimpleLock()

  val taskPool = SimpleTaskPool()

  override fun createUpdateGroup(key: Any, order: Int): UpdateGroup = lock.withLock {
    val result = ForkJoinPoolUpdateGroup(this, key, order)
    if (groups.putIfAbsent(key, result) != null)
      throw IllegalStateException("group with key $key already exists")
    orderedGroupsCache = null
    result
  }

  override fun removeUpdateGroup(key: Any): Unit = lock.withLock {
    val group = groups.remove(key) ?: return
    orderedGroupsCache = null
    group.onRemoved()
  }

  override fun update(): Unit = lock.withLock {
    if (orderedGroupsCache == null) {
      @Suppress("UNCHECKED_CAST")
      orderedGroupsCache = groups.values.sortedBy { it.order } as List<ForkJoinPoolUpdateGroup>
    }
    val orderedGroups = orderedGroupsCache ?: throw IllegalStateException("orderedGroupsCache null")
    if (orderedGroups.isEmpty())
      return
    var orderAt = orderedGroups[0].order
    for (group in orderedGroups) {
      group.reinitialize()
      when {
        // if it's the same order, then execute
        orderAt == group.order -> pool.execute(group)
        // if the next order is less than the current order, then
        // the groups are out of order and we should abort
        orderAt > group.order -> throw IllegalStateException("groups out of order")
        // wait for the current tasks to finish, then start the next group
        else -> {
          if (!pool.awaitQuiescence(5, TimeUnit.SECONDS))
            throw IllegalStateException("pool update never finished")
          orderAt = group.order
          pool.execute(group)
        }
      }
    }
    if (!pool.awaitQuiescence(5, TimeUnit.SECONDS))
      throw IllegalStateException("pool update never finished")
  }

  override fun shutdown() {
    if (ownsPool)
      pool.shutdownNow()
  }
}

class ForkJoinPoolUpdateGroup(
    private val engine: ForkJoinPoolUpdateEngine,
    override val key: Any,
    override val order: Int
) : RecursiveAction(), UpdateGroup {

  private val tasks = linkedMapOf<Any, ForkJoinPoolUpdateTask>()

  override fun registerJob(job: UpdateJob): Unit = engine.lock.withLock {
    if (tasks.putIfAbsent(job.key, engine.taskPool.obtain(job)) != null)
      throw IllegalStateException("job key already present: $job")
  }

  override fun removeJob(key: Any): Unit = engine.lock.withLock {
    val task = tasks[key] ?: return
    task.job?.onRemoved()
    engine.taskPool.release(task)
  }

  override fun compute() {
    val inPool = ForkJoinTask.inForkJoinPool()
    if (inPool) {
      tasks.values.forEach {
        it.reinitialize()
        it.fork()
      }
    }
    else {
      tasks.values.forEach {
        it.reinitialize()
        engine.pool.execute(it)
      }
    }
  }

  override fun onRemoved() {
    tasks.values.forEach { task ->
      task.job?.onRemoved()
      engine.taskPool.release(task)
    }
  }
}



class ForkJoinPoolUpdateTask(var job: UpdateJob?) : RecursiveAction() {
  override fun compute() {
    val job = job ?: throw IllegalStateException("no job set")
    job.update()
  }
}

class SimpleTaskPool(private val maxPooled: Int = 100) {
  private val pool = ArrayDeque<ForkJoinPoolUpdateTask>()

  fun release(task: ForkJoinPoolUpdateTask) {
    task.job = null
    if (pool.size < maxPooled)
      pool += task
  }

  fun obtain(job: UpdateJob): ForkJoinPoolUpdateTask {
    return pool.removeFirstOrNull() ?: ForkJoinPoolUpdateTask(job)
  }
}

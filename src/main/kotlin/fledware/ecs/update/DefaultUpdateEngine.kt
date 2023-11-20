package fledware.ecs.update

import fledware.ecs.util.OrderedMap

class DefaultUpdateEngine : UpdateEngine {
  private val _groups = OrderedMap<Any, DefaultUpdateGroup>()

  override val groups: Map<Any, UpdateGroup>
    get() = _groups.map

  override fun createUpdateGroup(key: Any, order: Int): UpdateGroup {
    val result = DefaultUpdateGroup(key, order)
    if (_groups.putIfAbsent(key, result) != null)
      throw IllegalStateException("group with key $key already exists")
    return result
  }

  override fun removeUpdateGroup(key: Any) {
    _groups.remove(key)
  }

  override fun update() {
    _groups.orderedValues().forEach { it.update() }
  }

  override fun shutdown() {
  }
}

class DefaultUpdateGroup(
    override val key: Any,
    override val order: Int
) : UpdateGroup {
  private val _jobs = OrderedMap<Any, DefaultUpdateJob>()

  fun update() {
    _jobs.orderedValues().forEach { it.update() }
  }

  override fun registerJob(job: UpdateJob) {
    job as? DefaultUpdateJob ?: throw IllegalArgumentException("job must be DefaultUpdateJob")
    if (_jobs.putIfAbsent(job.key, job) != null)
      throw IllegalStateException("job key already present: $job")
  }

  override fun removeJob(key: Any) {
    _jobs.remove(key)
  }

  override fun onRemoved() {
    _jobs.map.values.forEach { it.onRemoved() }
  }
}

abstract class DefaultUpdateJob(
    override val key: Any,
    override val order: Int
) : UpdateJob {
  override fun onRemoved() = Unit
}

fun DefaultUpdateJob(key: Any, order: Int, block: (job: UpdateJob) -> Unit) =
    object : DefaultUpdateJob(key, order) {
      override fun update() {
        block(this)
      }
    }

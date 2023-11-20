//@file:Suppress("MemberVisibilityCanBePrivate")
//
//package fledware.ecs.update
//
//import fledware.ecs.util.OrderedValue
//import fledware.ecs.util.SortedList
//import java.util.concurrent.ConcurrentLinkedQueue
//import java.util.concurrent.ForkJoinPool
//import java.util.concurrent.RecursiveAction
//import java.util.concurrent.TimeUnit
//
//class ThreadedUpdateEngine(
//    val pool: ForkJoinPool = ForkJoinPool()
//) : UpdateEngine {
//
//  val lock = SimpleLock()
//  val exceptions = ConcurrentLinkedQueue<Throwable>()
//  override val groups = mutableMapOf<Any, ThreadedUpdateGroup>()
//  val groupSets = SortedList<ThreadedUpdateNodeSet<ThreadedUpdateGroup>>()
//
//  override fun createUpdateGroup(key: Any, order: Int): UpdateGroup {
//    val result = ThreadedUpdateGroup(this, key, order)
//    if (groups.putIfAbsent(key, result) != null)
//      throw IllegalArgumentException("group already exists: $key")
//    val set = groupSets.getByOrderOrPut(order) { ThreadedUpdateNodeSet(order) }
//    set.groupsInSet.add(result)
//    return result
//  }
//
//  override fun removeUpdateGroup(key: Any) {
//    val removing = groups.remove(key) ?: return
//    removing.onRemoved()
//    val set = groupSets.getByOrder(removing.order)
//    set.groupsInSet.remove(removing)
//    if (set.groupsInSet.isEmpty())
//      groupSets.remove(set)
//  }
//
//  override fun update(): Unit = lock.withLock {
//    if (groupSets.isEmpty())
//      return
//    for (index in groupSets.indices) {
//      groupSets.getByIndex(index).next = groupSets.getByIndexOrNull(index + 1)
//    }
//    groupSets.getByIndex(0).start()
//    if (!pool.awaitQuiescence(5, TimeUnit.SECONDS)) {
//      pool.shutdownNow()
//      throw IllegalStateException("pool update never finished")
//    }
//  }
//
//  override fun shutdown() {
//    pool.shutdownNow()
//  }
//}
//
//
//
//class ThreadedUpdateNodeSet<T : ThreadedUpdateNode>(
//    override val order: Int
//): OrderedValue {
//  val link = CounterWorkLink()
//  val groupsInSet = mutableListOf<T>()
//  var next: ThreadedUpdateNodeSet<*>? = null
//
//  fun start() {
//    link.reset(groupsInSet.size) { next?.start() }
//    groupsInSet.forEach {
//      it.parentLink = link
//      it.resetAndStart()
//    }
//  }
//}
//
//abstract class ThreadedUpdateNode : RecursiveAction(), UpdateNode {
//  abstract val engine: ThreadedUpdateEngine
//  var parentLink: CounterWorkLink? = null
//
//  fun resetAndStart() {
//    this.reinitialize()
//    engine.pool.submit(this)
//  }
//
//  fun notifyFinished() {
//    parentLink?.workFinished()
//  }
//}
//
//open class ThreadedUpdateGroup(
//    override val engine: ThreadedUpdateEngine,
//    override val key: Any,
//    override val order: Int,
//) : ThreadedUpdateNode(), UpdateGroup {
//  val jobs = mutableMapOf<Any, ThreadedUpdateJob>()
//  val jobsSets = SortedList<ThreadedUpdateNodeSet<ThreadedUpdateJob>>()
//
//  override fun registerJob(job: UpdateJob) {
//    val actualJob = job as? ThreadedUpdateJob ?: ThreadedUpdateJobAdapter(engine, job)
//    if (jobs.putIfAbsent(key, actualJob) != null)
//      throw IllegalArgumentException("group already exists: $key")
//    val set = jobsSets.getByOrderOrPut(order) { ThreadedUpdateNodeSet(order) }
//    set.groupsInSet.add(actualJob)
//  }
//
//  override fun removeJob(key: Any) {
//    val removing = jobs.remove(key) ?: return
//    removing.onRemoved()
//    val set = jobsSets.getByOrder(removing.order)
//    set.groupsInSet.remove(removing)
//    if (set.groupsInSet.isEmpty())
//      jobsSets.remove(set)
//  }
//
//  override fun compute() {
//    if (jobsSets.isEmpty())
//      return
//    for (index in jobsSets.indices) {
//      jobsSets.getByIndex(index).next = jobsSets.getByIndexOrNull(index + 1)
//    }
//    jobsSets.getByIndex(0).start()
//  }
//
//  override fun onRemoved() {
//    jobs.values.forEach { it.onRemoved() }
//    jobs.clear()
//    jobsSets.clear()
//  }
//}
//
//open class ThreadedUpdateJob(
//
//) : ThreadedUpdateNode(), UpdateJob
//
//class ThreadedUpdateJobAdapter(
//    engine: ThreadedUpdateEngine,
//    val wrapping: UpdateJob
//) : ThreadedUpdateJob(engine, wrapping.key, wrapping.order) {
//  override fun update() {
//    wrapping.update()
//  }
//
//  override fun onRemoved() {
//    wrapping.onRemoved()
//  }
//}
//
//
//
//
//
//
////class ThreadedNodeTaskManager(val sharedLock: SimpleLock, on) {
////  val nodes = mutableMapOf<Any, ThreadedNodeTask>()
////  val sets = OrderedMap<Int, ThreadedNodeSet>()
////
////  fun add(task: ThreadedNodeTask): Unit = sharedLock.withLock {
////    if (nodes.putIfAbsent(task.key, task) != null)
////      throw IllegalStateException("group with key ${task.key} already exists")
////
////    val set = sets.getOrPut(task.order) { ThreadedNodeSet(task.order) }
////    set.startingTasks += task
////  }
////
////  fun remove(key: Any): Unit = sharedLock.withLock {
////    val node = nodes.remove(key) ?: return
////    node.onRemoved()
////    val set = sets[node.order] ?: throw IllegalStateException("unable to find node set")
////    set.startingTasks -= node
////    if (set.startingTasks.isEmpty())
////      sets.remove(node.order)
////  }
////
////  fun clear(): Unit = sharedLock.withLock {
////    nodes.values.forEach { it.onRemoved() }
////    nodes.clear()
////    sets.clear()
////  }
////
////  fun start() {
////    if (nodes.isEmpty())
////      return
////    if (sets.orderedValuesCache == null) {
////      val orderedSets = sets.orderedValues()
////      for (index in orderedSets.indices) {
////        orderedSets[index].nextSet = orderedSets.getOrNull(index + 1)
////      }
////    }
////    sets.orderedValues()[0].start()
////  }
////}
////
////class ThreadedNodeSet(
////    override val order: Int
////) : OrderedValue {
////  val link = CounterWorkLink()
////  val startingTasks = mutableSetOf<ThreadedNodeTask>()
////  var nextSet: ThreadedNodeSet? = null
////
////  fun start() {
////    if (startingTasks.isEmpty()) {
////      nextSet?.start()
////      return
////    }
////    link.reset(startingTasks.size) { nextSet?.start() }
////    startingTasks.forEach { it.resetAndSubmit(link) }
////  }
////}
////
////
//////abstract class ThreadedWorkNode(
//////    val parent: ThreadedWorkNode?,
//////    val exceptions: Queue<Throwable>
//////) {
//////  val children = mutableListOf<ThreadedWorkNode>()
//////  val workingAt = AtomicInteger()
//////  val workingRequired = AtomicInteger()
//////
//////  inline fun executeAndCount(block: () -> Unit) {
//////    try {
//////      block()
//////    }
//////    catch (ex: Throwable) {
//////      exceptions += ex
//////    }
//////    finally {
//////      if (workingAt.incrementAndGet() == workingRequired.get())
//////        doNext()
//////    }
//////  }
//////
//////  fun addWork(count: Int) {
//////    workingRequired.getAndAdd(count)
//////  }
//////
//////  fun reset() {
//////    workingAt.set(0)
//////    workingRequired.set(children.size)
//////
//////
//////  }
//////
//////  fun finishWorkUnit() {
//////    if (workingAt.incrementAndGet() == workingRequired.get())
//////      doNext()
//////  }
//////
//////  fun doNext() {
//////    val parent = parent ?: return
//////    parent.finishWorkUnit()
//////  }
//////}
////
////
////abstract class ThreadedTask(val pool: ForkJoinPool) : RecursiveAction() {
////  var parentLink: CounterWorkLink? = null
////  var link: CounterWorkLink? = null
////
////  fun resetAndSubmit(link: CounterWorkLink) {
////    this.link = link
////    this.reinitialize()
////    pool.submit(this)
////  }
////
////  final override fun compute() {
////    val link = link ?: throw IllegalStateException("no link")
////    link.executeAndCount { entryFromFork() }
////  }
////
////  abstract fun entryFromFork()
////}
////
////abstract class ThreadedNodeTask(
////    val engine: ThreadedUpdateEngine,
////    override val key: Any,
////    override val order: Int
////) : ThreadedTask(engine.pool), UpdateNode {
////
////  fun submitWork(task: ThreadedTask) {
////    val link = link ?: throw IllegalStateException("no link")
////    link.addWork(1)
////    task.resetAndSubmit(link)
////  }
////
////  fun submitWork(tasks: Collection<ThreadedTask>) {
////    val link = link ?: throw IllegalStateException("no link")
////    link.addWork(tasks.size)
////    tasks.forEach { it.resetAndSubmit(link) }
////  }
////}
////
////
////class ThreadedUpdateGroup(
////    engine: ThreadedUpdateEngine, key: Any, order: Int,
////    val groupLink: CounterWorkLink
////) : ThreadedNodeTask(engine, key, order), UpdateGroup {
////
////  val jobs = ThreadedNodeTaskManager(engine.lock)
////
////  override fun registerJob(job: UpdateJob) {
////    val actualJob = job as? ThreadedUpdateJob ?: ThreadedUpdateJobAdapter(engine, job)
////    jobs.
////  }
////
////  override fun removeJob(key: Any) {
////    TODO("Not yet implemented")
////  }
////
////  override fun onRemoved() {
////    TODO("Not yet implemented")
////  }
////
////  override fun entryFromFork() {
////    submitWork(jobs.)
////  }
////}
////
////
////abstract class ThreadedUpdateJob(
////    engine: ThreadedUpdateEngine, key: Any, order: Int
////) : ThreadedNodeTask(engine, key, order), UpdateJob {
////  override fun entryFromFork() {
////    update()
////  }
////}
////
////class ThreadedUpdateJobAdapter(
////    engine: ThreadedUpdateEngine,
////    val wrapping: UpdateJob
////) : ThreadedUpdateJob(engine, wrapping.key, wrapping.order) {
////  override fun update() {
////    wrapping.update()
////  }
////
////  override fun onRemoved() {
////    wrapping.onRemoved()
////  }
////}

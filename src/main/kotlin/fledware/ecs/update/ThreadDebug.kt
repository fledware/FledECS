package fledware.ecs.update

/**
 * used to help debug issues with latches and whatnot because
 * making those turned out to be incredibly difficult.
 */
object ThreadDebug {
  const val debug = false
  inline fun log(message: () -> String) { if (debug) println(Thread.currentThread().name + ": " + message()) }
}

package fledware.ecs

/**
 * sent to the [System]s world when the [System.order] changes.
 */
data class OnSystemOrderChange(
    val system: System
)

data class OnSystemAdded(
    val system: System
)

data class OnSystemRemoved(
    val system: System
)

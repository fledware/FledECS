package fledware.ecs.world

import fledware.ecs.Entity
import fledware.ecs.OnCreateEntityWorldHook
import fledware.ecs.World
import fledware.ecs.WorldHooks
import fledware.ecs.util.SortedList

class DefaultWorldHooks : WorldHooks {
  override val onBeforeUpdate: MutableList<(World) -> Unit> = mutableListOf()
  override val onAfterUpdate: MutableList<(World) -> Unit> = mutableListOf()
  override val onDeleteEntity: MutableList<(world: World, entity: Entity) -> Unit> = mutableListOf()
  override val onCreateEntity: SortedList<OnCreateEntityWorldHook> = SortedList()
  override val onWorldDestroy: MutableList<(World) -> Unit> = mutableListOf()
}
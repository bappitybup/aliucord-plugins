package com.example.servericonlongpressfix

import android.content.Context
import android.view.*
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.*
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.*
import com.discord.utilities.view.extensions.RecyclerViewExtensionsKt
import com.discord.widgets.guilds.contextmenu.*
import com.discord.widgets.guilds.list.GuildListViewHolder.FolderViewHolder
import com.discord.widgets.guilds.list.GuildsDragAndDropCallback

@AliucordPlugin
class ServerIconLongPressFix : Plugin() {
    private var holder: RecyclerView.ViewHolder? = null
    private var held = false

    override fun start(context: Context) {
        patcher.before<ItemTouchHelper.Callback>(
            "hasDragFlag", RecyclerView::class.java, RecyclerView.ViewHolder::class.java,
        ) { (p, _: RecyclerView, guild: RecyclerView.ViewHolder) ->
            if (guild === holder) p.result = false
        }

        patcher.before<RecyclerViewExtensionsKt?>(
            "ignoreCurrentTouch", RecyclerView::class.java,
        ) { (param, recyclerView: RecyclerView) ->
            val guild = holder ?: return@before
            if (guild.itemView.parent === recyclerView) {
                held = true
                param.result = null
            }
        }

        patcher.before<ItemTouchHelper>("findAnimation", MotionEvent::class.java) { (_, event: MotionEvent) ->
            if (mCallback !is GuildsDragAndDropCallback) return@before
            held = false
            holder = mRecyclerView.findChildViewUnder(event.x, event.y)
                ?.let(mRecyclerView::getChildViewHolder)
        }

        patcher.before<ItemTouchHelper>(
            "checkSelectForSwipe", Int::class.javaPrimitiveType!!,
            MotionEvent::class.java, Int::class.javaPrimitiveType!!,
        ) { (_, action: Int, _: MotionEvent, _: Int) ->
            val drag = holder ?: return@before
            if (mCallback !is GuildsDragAndDropCallback || !held ||
                action != MotionEvent.ACTION_MOVE ||
                drag !is GuildsDragAndDropCallback.DraggableViewHolder || !drag.canDrag()) return@before
            holder = null
            hideMenu(drag)
            startDrag(drag)
        }
    }

    private fun hideMenu(holder: RecyclerView.ViewHolder) {
        if (holder is FolderViewHolder)
            WidgetFolderContextMenu.Companion!!.hide(Utils.appActivity, false)
        else
            WidgetGuildContextMenu.Companion!!.hide(Utils.appActivity, false)
    }

    private fun clear() { holder = null }
    override fun stop(context: Context) { patcher.unpatchAll(); clear() }
}

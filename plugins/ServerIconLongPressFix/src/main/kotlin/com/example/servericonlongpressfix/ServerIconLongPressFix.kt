package com.example.servericonlongpressfix

import android.content.Context
import android.view.*
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.*
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.*
import com.discord.widgets.guilds.contextmenu.WidgetGuildContextMenu
import com.discord.widgets.guilds.list.GuildListViewHolder.GuildViewHolder
import com.discord.widgets.guilds.list.GuildsDragAndDropCallback

@AliucordPlugin
class ServerIconLongPressFix : Plugin() {
    private var helper: ItemTouchHelper? = null
    private var holder: GuildViewHolder? = null
    private var held = false

    override fun start(context: Context) {
        patcher.before<ItemTouchHelper.Callback>(
            "hasDragFlag", RecyclerView::class.java, RecyclerView.ViewHolder::class.java,
        ) { p -> if (p.args[1] === holder) p.result = false }

        patcher.patch(
            "com.discord.utilities.view.extensions.RecyclerViewExtensionsKt", "ignoreCurrentTouch",
            arrayOf(RecyclerView::class.java), PreHook { p ->
                if (holder != null && helper?.mRecyclerView === p.args[0]) { held = true; p.result = null }
            },
        )

        patcher.after<ItemTouchHelper>("attachToRecyclerView", RecyclerView::class.java) {
            if (mCallback !is GuildsDragAndDropCallback) return@after
            if (mRecyclerView != null) helper = this else if (helper === this) clear()
        }

        patcher.before<ViewGroup>("dispatchTouchEvent", MotionEvent::class.java) { p ->
            if (this === helper?.mRecyclerView) touch(p.args[0] as MotionEvent)
        }
    }

    private fun touch(e: MotionEvent) {
        val recycler = helper?.mRecyclerView ?: return
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                held = false
                holder = recycler.findChildViewUnder(e.x, e.y)?.let(recycler::getChildViewHolder) as? GuildViewHolder
            }
            MotionEvent.ACTION_MOVE -> {
                val drag = holder ?: return
                if (held && drag.canDrag()) { holder = null; hideMenu(); helper?.startDrag(drag) }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> holder = null
        }
    }

    private fun hideMenu() {
        try {
            val menu = WidgetGuildContextMenu::class.java.getField("INSTANCE")[null]
            menu.javaClass.getMethod("hide", FragmentActivity::class.java, Boolean::class.java)(menu, Utils.appActivity, false)
        } catch (e: Throwable) { logger.warn("Menu hide failed", e) }
    }

    private fun clear() { helper = null; holder = null }
    override fun stop(context: Context) { patcher.unpatchAll(); clear() }
}

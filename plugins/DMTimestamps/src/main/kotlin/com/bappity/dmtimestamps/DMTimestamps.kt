package com.bappity.dmtimestamps

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.*
import com.aliucord.utils.DimenUtils.dp
import com.aliucord.utils.ViewUtils.addTo
import com.aliucord.utils.ViewUtils.findViewById
import com.aliucord.wrappers.ChannelWrapper.Companion.id
import com.discord.stores.StoreMessagesMostRecent
import com.discord.stores.StoreStream
import com.discord.utilities.SnowflakeUtils
import com.discord.widgets.channels.list.WidgetChannelsListAdapter
import com.discord.widgets.channels.list.items.ChannelListItem
import com.discord.widgets.channels.list.items.ChannelListItemPrivate
import java.util.WeakHashMap

private const val DM_ROW_END_TAG = "DMRowTopEnd"
private const val PRIORITY = 10

@AliucordPlugin
class DMTimestamps : Plugin() {
    private val rows = WeakHashMap<TextView, Long>()

    override fun start(context: Context) {
        val store = StoreStream.getMessagesMostRecent()

        fun update(label: TextView, channelId: Long) {
            val age = store.mostRecentIds[channelId]?.let { formatAge(SnowflakeUtils.toTimestamp(it)) }
            label.visibility = if (age == null) View.GONE else View.VISIBLE
            age?.let { label.text = it }
            val end = label.parent as? LinearLayout
            if (end != null)
                updateEndMargin(end)
        }

        // update all rows timestamps when the most recent message changes
        patcher.after<StoreMessagesMostRecent>("snapshotData") {
            Utils.mainThread.post { rows.forEach { (label, channelId) -> update(label, channelId) } }
        }

        // show most recent message age on every dm row
        patcher.after<WidgetChannelsListAdapter.ItemChannelPrivate>(
            "onConfigure", Int::class.java, ChannelListItem::class.java,
        ) { (_, _: Int, item: ChannelListItem) ->
            val row = itemView as RelativeLayout
            val name = row.findViewById<TextView>("channels_list_item_private_name")
            val end = getEnd(row)
            val label = end.findViewWithTag<TextView>("DMTimestamps")
                ?: TextView(end.context).apply {
                    tag = "DMTimestamps"
                    setTag(name.id, PRIORITY)
                    textSize = 10f
                    var index = end.childCount
                    for (i in 0 until end.childCount) {
                        val priority = end.getChildAt(i).getTag(name.id) as? Int ?: 0
                        if (PRIORITY > priority) {
                            index = i
                            break
                        }
                    }
                    end.addView(this, index)
                }

            val channelId = (item as ChannelListItemPrivate).channel.id
            rows[label] = channelId
            val color = name.currentTextColor
            label.setTextColor(if (color ushr 24 == 0) 0xFFFFFFFF.toInt() else color)
            label.alpha = 0.7f
            update(label, channelId)
        }
    }

    private fun updateEndMargin(end: LinearLayout) {
        val row = end.parent as? RelativeLayout ?: return
        val content = row.findViewById<TextView>("channels_list_item_private_name").parent.parent as LinearLayout
        val params = content.layoutParams as RelativeLayout.LayoutParams

        end.measure(0, 0)
        params.marginEnd = 16.dp + end.measuredWidth
        content.layoutParams = params
    }

    private fun getEnd(row: RelativeLayout) =
        row.findViewWithTag<LinearLayout>(DM_ROW_END_TAG)
            ?: LinearLayout(row.context).addTo(row) {
                tag = DM_ROW_END_TAG
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,16.dp).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_TOP)
                    addRule(RelativeLayout.ALIGN_PARENT_END)
                    topMargin = 2.dp
                    marginEnd = 8.dp
                }
            }

    private fun formatAge(timestamp: Long): String {
        val minutes = ((System.currentTimeMillis() - timestamp) / 60_000).coerceAtLeast(1)

        return when {
            minutes < 60 -> "${minutes}m"
            minutes < 1_440 -> "${minutes / 60}h"
            minutes < 43_200 -> "${minutes / 1_440}d"
            minutes < 525_600 -> "${minutes / 43_200}mo"
            else -> "${minutes / 525_600}y"
        }
    }

    override fun stop(context: Context) { patcher.unpatchAll(); rows.clear() }
}

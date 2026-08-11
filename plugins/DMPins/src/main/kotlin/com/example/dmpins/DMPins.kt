package com.example.dmpins

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.*
import com.aliucord.utils.DimenUtils.dp
import com.aliucord.utils.RxUtils.await
import com.aliucord.utils.ViewUtils.addTo
import com.aliucord.utils.ViewUtils.findViewById
import com.aliucord.wrappers.ChannelWrapper.Companion.id
import com.aliucord.wrappers.ChannelWrapper.Companion.isDM
import com.discord.restapi.RestAPIParams
import com.discord.stores.StoreStream
import com.discord.utilities.rest.RestAPI
import com.discord.widgets.channels.list.WidgetChannelListModel
import com.discord.widgets.channels.list.WidgetChannelsList
import com.discord.widgets.channels.list.WidgetChannelsListAdapter
import com.discord.widgets.channels.list.WidgetChannelsListItemChannelActions
import com.discord.widgets.channels.list.items.ChannelListItem
import com.discord.widgets.channels.list.items.ChannelListItemPrivate
import com.lytefast.flexinput.R
import java.util.WeakHashMap

private const val PRIVATE_CHANNELS_ID = 0L
private const val PINNED_FLAG = 0x800
private const val DM_ROW_END_TAG = "DMRowTopEnd"

@AliucordPlugin
class DMPins : Plugin() {
    private val pinIcons = WeakHashMap<ImageView, Long>()
    override fun start(context: Context) {
        // add pin action to dm context menu
        patcher.after<WidgetChannelsListItemChannelActions>(
            "configureUI", WidgetChannelsListItemChannelActions.Model::class.java,
        ) { (_, model: WidgetChannelsListItemChannelActions.Model) ->
            val channel = model.channel
            if (!channel.isDM()) return@after

            val pinned = isPinned(channel.id)

            requireView()
                .findViewById<TextView>("text_action_thread_browser")
                .apply {
                    visibility = View.VISIBLE
                    text = if (pinned) "Unpin" else "Pin"
                    setCompoundDrawablesWithIntrinsicBounds(R.e.ic_pin_24dp, 0, 0, 0)
                    setOnClickListener {
                        setPinned(channel.id, !pinned)
                        dismiss()
                    }
                }
        }

        // show glyph on pinned dm rows
        patcher.after<WidgetChannelsListAdapter.ItemChannelPrivate>(
            "onConfigure", Int::class.java, ChannelListItem::class.java,
        ) { (_, _: Int, item: ChannelListItem) ->
            val row = itemView as RelativeLayout
            val name = row.findViewById<TextView>("channels_list_item_private_name")
            val icon = row.findViewWithTag<ImageView>("DMPins")
                ?: ImageView(row.context).addTo(row) {
                    tag = "DMPins"
                    setImageResource(R.e.ic_pin_24dp)
                    layoutParams = RelativeLayout.LayoutParams(12.dp, 12.dp).apply {
                        addRule(RelativeLayout.ALIGN_PARENT_TOP)
                        addRule(RelativeLayout.ALIGN_PARENT_END)
                        topMargin = 4.dp
                        marginEnd = 8.dp
                    }
                }
            val params = icon.layoutParams as RelativeLayout.LayoutParams
            val end = row.findViewWithTag<View>(DM_ROW_END_TAG)

            if (end != null) {
                if (end.id == View.NO_ID)
                    end.id = View.generateViewId()
                
                params.removeRule(RelativeLayout.ALIGN_PARENT_END)
                params.addRule(RelativeLayout.START_OF, end.id)
                params.marginEnd = 4.dp
            } else {
                params.removeRule(RelativeLayout.START_OF)
                params.addRule(RelativeLayout.ALIGN_PARENT_END)
                params.marginEnd = 8.dp
            }
            icon.layoutParams = params
            
            val id = (item as ChannelListItemPrivate).channel.id
            pinIcons[icon] = id
            icon.visibility = if (isPinned(id)) View.VISIBLE else View.GONE
            icon.setColorFilter(name.currentTextColor)
        }

        // sort dm rows
        patcher.before<WidgetChannelsList>(
            "configureUI", WidgetChannelListModel::class.java,
        ) { (param, model: WidgetChannelListModel) ->
            if (model.isGuildSelected) return@before

            val (pinnedDMs, otherDMs) = model.items.partition { item ->
                item is ChannelListItemPrivate && isPinned(item.channel.id)
            }

            val items = pinnedDMs + otherDMs
            if (items == model.items) return@before

            param.args[0] = model.copy(
                model.selectedGuild,
                items,
                model.isGuildSelected,
                model.showPremiumGuildHint,
                model.showEmptyState,
                model.guildScheduledEvents,
            )
        }
    }

    private fun getFlags(id: Long) = StoreStream.getUserGuildSettings()
        .guildSettings[PRIVATE_CHANNELS_ID]
        ?.getChannelOverride(id)
        ?.flags
        ?: 0

    private fun isPinned(id: Long) =
        getFlags(id) and PINNED_FLAG != 0

    private fun setPinned(id: Long, pinned: Boolean) {
        val flags = getFlags(id)
        val newFlags = if (pinned) flags or PINNED_FLAG else flags and PINNED_FLAG.inv()
        val icon = pinIcons.entries.firstOrNull { it.value == id }?.key
        icon?.visibility = if (pinned) View.VISIBLE else View.GONE

        Utils.threadPool.execute {
            val settings = RestAPIParams.UserGuildSettings(id, RestAPIParams.UserGuildSettings.ChannelOverride(null, newFlags))

            val error = RestAPI.getApi()
                .updateUserGuildSettings(PRIVATE_CHANNELS_ID, settings)
                .await()
                .second

            if (error != null) {
                icon?.let { pinIcon ->
                    pinIcon.post {
                        if (pinIcons[pinIcon] == id)
                            pinIcon.visibility = if (pinned) View.GONE else View.VISIBLE
                    }
                }

                logger.error("Failed to update DM pin state for $id", error)
            }
        }
    }

    override fun stop(context: Context) { patcher.unpatchAll(); pinIcons.clear() }
}

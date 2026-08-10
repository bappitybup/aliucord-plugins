package com.example.dmpins

import android.content.Context
import android.view.View
import android.widget.TextView
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.*
import com.aliucord.utils.RxUtils.await
import com.aliucord.utils.ViewUtils.findViewById
import com.aliucord.wrappers.ChannelWrapper.Companion.id
import com.aliucord.wrappers.ChannelWrapper.Companion.isDM
import com.discord.restapi.RestAPIParams
import com.discord.stores.StoreStream
import com.discord.utilities.rest.RestAPI
import com.discord.widgets.channels.list.WidgetChannelsListItemChannelActions
import com.discord.widgets.channels.list.WidgetChannelListModel
import com.discord.widgets.channels.list.WidgetChannelsList
import com.discord.widgets.channels.list.items.ChannelListItemPrivate
import com.lytefast.flexinput.R

private const val PRIVATE_CHANNELS_ID = 0L
private const val PINNED_FLAG = 0x800

@AliucordPlugin
class DMPins : Plugin() {
    override fun start(context: Context) {
        // add pin action to dm context menu
        patcher.after<WidgetChannelsListItemChannelActions>(
            "configureUI", WidgetChannelsListItemChannelActions.Model::class.java,
        ) { (_, model: WidgetChannelsListItemChannelActions.Model) ->
            val channel = model.channel
            if (!channel.isDM()) return@after

            val pinned = getFlags(channel.id) and PINNED_FLAG != 0

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

        // sort dm rows
        patcher.before<WidgetChannelsList>(
            "configureUI", WidgetChannelListModel::class.java,
        ) { (param, model: WidgetChannelListModel) ->
            if (model.isGuildSelected) return@before

            val (pinnedDMs, otherDMs) = model.items.partition { item ->
                item is ChannelListItemPrivate && isPinned(item.channel.id)
            }

            val items = pinnedDMs + otherDMs

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

        Utils.threadPool.execute {
            val settings = RestAPIParams.UserGuildSettings(id, RestAPIParams.UserGuildSettings.ChannelOverride(null, newFlags))

            val error = RestAPI.getApi()
                .updateUserGuildSettings(PRIVATE_CHANNELS_ID, settings)
                .await()
                .second

            if (error != null)
                logger.error("Failed to update DM pin state for $id", error)
        }
    }

    override fun stop(context: Context) { patcher.unpatchAll() }
}

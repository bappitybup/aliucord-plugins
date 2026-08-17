package com.example.uncappedtimestamps

import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after
import com.aliucord.utils.DimenUtils.dp
import com.aliucord.utils.accessField
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage
import com.discord.widgets.chat.list.entries.ChatListEntry

private val WidgetChatListAdapterItemMessage.itemName by accessField<TextView?>()
private val WidgetChatListAdapterItemMessage.itemTag by accessField<TextView?>()
private val WidgetChatListAdapterItemMessage.itemTimestamp by accessField<TextView?>()
private val WidgetChatListAdapterItemMessage.itemText by accessField<TextView>()

@AliucordPlugin
class UncappedTimestamps : Plugin() {
    override fun start(context: Context) {
        patcher.after<WidgetChatListAdapterItemMessage>(
            "onConfigure", Int::class.java, ChatListEntry::class.java,
        ) {
            val name = itemName ?: return@after
            val tag = itemTag ?: return@after
            val timestamp = itemTimestamp ?: return@after
            val header = name.parent as? ConstraintLayout ?: return@after

            timestamp.allowWrapping(header, name, tag)
            itemText.below(header.id)
            if (header.width > 0) {
                header.measure(android.view.View.MeasureSpec.makeMeasureSpec(header.width, android.view.View.MeasureSpec.EXACTLY), 0)
                if ((timestamp.layout?.lineCount ?: 1) > 1 && timestamp.moveBelow(header, name, tag)) itemText.below(timestamp.id)
            }
            timestamp.post {
                if (timestamp.parent !== header) return@post
                val wrapped = (timestamp.layout?.lineCount ?: 1) > 1
                if (wrapped && timestamp.moveBelow(header, name, tag)) {
                    itemText.below(timestamp.id)
                }
            }
        }
    }

    private fun TextView.allowWrapping(header: ConstraintLayout, name: TextView, tag: TextView) {
        header.minimumHeight = 0
        if (parent !== header) {
            (parent as? ViewGroup)?.removeView(this)
            header.addView(this)
        }

        maxWidth = Int.MAX_VALUE
        setSingleLine(false)
        ellipsize = null
        setHorizontallyScrolling(false)

        layoutParams = (layoutParams as ConstraintLayout.LayoutParams).apply {
            width = 0
            marginStart = 6.dp
            startToStart = ConstraintLayout.LayoutParams.UNSET
            startToEnd = tag.id
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToBottom = ConstraintLayout.LayoutParams.UNSET
            baselineToBaseline = name.id
        }
        requestLayout()
    }


    private fun TextView.moveBelow(header: ConstraintLayout, name: TextView, tag: TextView): Boolean {
        val outer = header.parent as? ConstraintLayout ?: return false
        val tagBottomMargin = (tag.layoutParams as ConstraintLayout.LayoutParams).bottomMargin

        if (parent !== outer) {
            (parent as? ViewGroup)?.removeView(this)
            outer.addView(this)
        }

        header.minimumHeight = name.bottom + tagBottomMargin

        layoutParams = (layoutParams as ConstraintLayout.LayoutParams).apply {
            width = 0
            marginStart = name.left
            startToEnd = ConstraintLayout.LayoutParams.UNSET
            baselineToBaseline = ConstraintLayout.LayoutParams.UNSET
            startToStart = header.id
            endToEnd = header.id
            topToBottom = header.id
        }
        requestLayout()
        return true
    }

    private fun TextView.below(viewId: Int) {
        (layoutParams as ConstraintLayout.LayoutParams).topToBottom = viewId
        requestLayout()
    }

    override fun stop(context: Context) = patcher.unpatchAll()
}

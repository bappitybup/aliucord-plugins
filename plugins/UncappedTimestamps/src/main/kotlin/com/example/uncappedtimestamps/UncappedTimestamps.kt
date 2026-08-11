package com.example.uncappedtimestamps

import android.content.Context
import android.text.Layout
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

            timestamp.post {
                val inline = timestamp.placeTimestamp(header, name, tag)
                itemText.below(if (inline) header.id else timestamp.id)
            }
        }
    }

    private fun TextView.placeTimestamp(
        header: ConstraintLayout,
        name: TextView,
        tag: TextView,
    ): Boolean {
        val desiredWidth =
            Layout.getDesiredWidth(text, paint) +
                compoundPaddingLeft +
                compoundPaddingRight

        val availableWidth =
            header.width - left

        val inline =
            !text.contains('\n') &&
                desiredWidth <= availableWidth

        val parent = if (inline) {
            header
        } else {
            val outer = header.parent
            outer as? ConstraintLayout ?: run {
                return true
            }
        }

        if (this.parent !== parent) {
            (this.parent as? ViewGroup)?.removeView(this)
            parent.addView(this)
        }

        maxWidth = Int.MAX_VALUE
        setSingleLine(inline)
        maxLines = if (inline) 1 else Int.MAX_VALUE
        ellipsize = null
        setHorizontallyScrolling(false)

        layoutParams = (layoutParams as ConstraintLayout.LayoutParams).apply {
                width = 0
                startToStart = ConstraintLayout.LayoutParams.UNSET
                startToEnd = ConstraintLayout.LayoutParams.UNSET
                baselineToBaseline = ConstraintLayout.LayoutParams.UNSET
                topToBottom = ConstraintLayout.LayoutParams.UNSET

                if (inline) {
                    marginStart = 6.dp
                    startToEnd = tag.id
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    baselineToBaseline = name.id
                } else {
                    marginStart = name.left
                    startToStart = header.id
                    endToEnd = header.id
                    topToBottom = header.id
                }
            }

        return inline
    }

    private fun TextView.below(viewId: Int) {
        (layoutParams as ConstraintLayout.LayoutParams).topToBottom = viewId
        requestLayout()
    }

    private val TextView.contentWidth
        get() = width - paddingLeft - paddingRight

    override fun stop(context: Context) = patcher.unpatchAll()
}

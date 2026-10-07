package com.bappity.attachmentfocusfix

import android.content.Context
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after
import com.discord.widgets.chat.input.AppFlexInputViewModel
import com.lytefast.flexinput.FlexInputListener

@AliucordPlugin
class AttachmentFocusFix : Plugin() {
    override fun start(context: Context) {
        patcher.after<AppFlexInputViewModel>("onAttachmentsUpdated", List::class.java) { focus() }
        patcher.after<AppFlexInputViewModel>("onSendButtonClicked", FlexInputListener::class.java) { focus() }
    }

    override fun stop(context: Context) = patcher.unpatchAll()
}
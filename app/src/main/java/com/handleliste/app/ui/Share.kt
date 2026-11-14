package com.handleliste.app.ui

import android.content.Context
import android.content.Intent
import com.handleliste.app.data.ListRepo
import com.handleliste.app.data.Parse
import com.handleliste.app.util.tr

/**
 * Sharing a list: as plain text through any app (Messages, Messenger, Snapchat …). The other phone
 * shares that text to Handleliste (or pastes it), and it becomes a list there.
 */
object ShareList {
    fun send(ctx: Context, listId: String) {
        val l = ListRepo.lists.firstOrNull { it.id == listId } ?: return
        val items = ListRepo.itemsOf(listId).filter { !it.checked }.map { it.name }
        val text = Parse.listText(l.name, items, tr("Sent from Handleliste. Share this text to Handleliste to get the list.",
            "Sendt fra Handleliste. Del denne teksten til Handleliste for å få listen."))
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, l.name).putExtra(Intent.EXTRA_TEXT, text)
        ctx.startActivity(Intent.createChooser(send, tr("Share list", "Del listen")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Text shared to the app becomes a new list. Returns its name, or null if there was nothing in it. */
    fun receive(text: String): String? {
        val (name, items) = Parse.sharedList(text)
        if (items.isEmpty()) return null
        val n = name ?: tr("Shared list", "Delt liste")
        ListRepo.newListFrom(n, items, null)
        return n
    }
}

package ru.ifmo.bot.listners

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class MessageListener : ListenerAdapter() {

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) {
            return
        }

        val message = event.message.contentRaw

        if (message.equals("!ping", ignoreCase = true)) {
            event.channel.sendMessage("Pong!").queue()
        }
    }
}
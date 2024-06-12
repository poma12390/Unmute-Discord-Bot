package ru.ifmo.bot.listners

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.LoggerFactory
import java.io.InputStreamReader

class MessageListener(private val targetUserId: String,
                      private val shouldReplyToOwnerMentions: Boolean) : ListenerAdapter() {

    private val logger = LoggerFactory.getLogger(MessageListener::class.java)

    private val responses: List<String> by lazy {
        this::class.java.classLoader.getResourceAsStream("responses.txt")?.use { inputStream ->
            InputStreamReader(inputStream).readLines()
        } ?: throw IllegalArgumentException("Файл responses.txt не найден")
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) {
            return
        }

        val message = event.message.contentRaw
        if (message.startsWith("!ping", ignoreCase = true)) {
            val responseMessage = "Pong!"
            event.message.reply(responseMessage).queue()
            logger.info("Ответил пользователю ${event.author.name}: $responseMessage")
        }

        if (shouldReplyToOwnerMentions && event.message.mentions.users.any { it.id == targetUserId }) {
            val response = responses.random()
            event.message.reply(response).queue()
            logger.info("Ответил пользователю ${event.author.name} на упоминание владельца сервера: $response")
        }
    }
}
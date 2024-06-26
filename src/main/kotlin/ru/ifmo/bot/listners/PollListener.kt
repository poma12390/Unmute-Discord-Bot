package ru.ifmo.bot.listners

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import ru.ifmo.bot.data.Poll
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class PollListener: ListenerAdapter() {
    private val polls = ConcurrentHashMap<String, Poll>()

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name == "poll") {
            val pollName = event.getOption("name")?.asString ?: return
            val maxParticipants = event.getOption("participants")?.asInt ?: return

            if (maxParticipants <= 0) {
                event.reply("Недопустимое количество участников.").setEphemeral(true).queue()
                return
            }

            val poll = Poll(pollName, maxParticipants)
            polls[pollName] = poll
            event.reply("Опрос **$pollName** начат для **$maxParticipants** участников!")
                .addActionRow(
                    Button.primary("vote_$pollName", "Голосовать"),
                    Button.danger("unvote_$pollName", "Отменить голос")
                ).queue { hook ->
                    hook.retrieveOriginal().queue { message ->
                        polls[pollName] = poll.copy(messageId = message.id)
                    }
                }
        }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        val buttonId = event.button.id ?: return
        val parts = buttonId.split("_")

        if (parts[0] == "vote" || parts[0] == "unvote") {
            val pollName = parts[1]
            val poll = polls[pollName]

            if (poll != null) {
                val userId = event.user.id

                when (parts[0]) {
                    "vote" -> {
                        if (poll.participants.contains(userId)) {
                            event.reply("Вы уже проголосовали в этом опросе.").setEphemeral(true).queue()
                        } else {
                            poll.participants.add(userId)
                            updatePollMessage(event, poll)
                            event.deferEdit().queue() // Отправляем пустой ответ для предотвращения ошибки взаимодействия
                        }
                    }
                    "unvote" -> {
                        if (poll.participants.contains(userId)) {
                            poll.participants.remove(userId)
                            updatePollMessage(event, poll)
                            event.deferEdit().queue() // Отправляем пустой ответ для предотвращения ошибки взаимодействия
                        } else {
                            event.reply("Вы ещё не голосовали в этом опросе.").setEphemeral(true).queue()
                        }
                    }
                }
            } else {
                event.reply("Опрос не найден или уже завершён.").setEphemeral(true).queue()
            }
        }
    }

    private fun updatePollMessage(event: ButtonInteractionEvent, poll: Poll) {
        val participantsMentions = mutableListOf<String>()
        val unknownParticipants = mutableListOf<String>()

        val participantsCount = poll.participants.size
        val maxParticipants = poll.maxParticipants

        val futures = poll.participants.map { id ->
            event.jda.retrieveUserById(id).submit()
        }

        CompletableFuture.allOf(*futures.toTypedArray()).thenAccept {
            futures.forEach { future ->
                try {
                    val user = future.get()
                    participantsMentions.add(user.asMention)
                } catch (e: Exception) {
                    unknownParticipants.add("Неизвестный пользователь")
                }
            }

            val unknownParticipantsCount = unknownParticipants.size

            if (participantsCount >= maxParticipants) {
                event.channel.sendMessage("Опрос **${poll.name}** завершён. Участники: ${participantsMentions.joinToString(", ")}" +
                        if (unknownParticipantsCount > 0) ", и ещё $unknownParticipantsCount неизвестных участников" else "").queue()
                poll.messageId?.let {
                    event.channel.retrieveMessageById(it).queue { message ->
                        message.editMessageComponents(emptyList<ActionRow>()).queue()
                    }
                }
                polls.remove(poll.name)
            } else {
                poll.messageId?.let {
                    event.channel.retrieveMessageById(it).queue { message ->
                        val updatedContent = """
                        Опрос **${poll.name}**
                        Проголосовало $participantsCount из $maxParticipants
                        Участники: ${participantsMentions.joinToString(", ")}${if (unknownParticipantsCount > 0) ", и ещё $unknownParticipantsCount неизвестных участников" else ""}
                    """.trimIndent()
                        message.editMessage(updatedContent)
                            .setActionRow(
                                Button.primary("vote_${poll.name}", "Голосовать"),
                                Button.danger("unvote_${poll.name}", "Отменить голос")
                            ).queue()
                    }
                }
            }
        }
    }
}
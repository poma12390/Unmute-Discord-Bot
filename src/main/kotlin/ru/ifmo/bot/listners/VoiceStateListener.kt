package ru.ifmo.bot.listners

import jakarta.annotation.Nonnull
import net.dv8tion.jda.api.audit.ActionType
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceGuildDeafenEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceGuildMuteEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class VoiceStateListener(private val targetUserId: String) : ListenerAdapter() {

    private val logger: Logger = LoggerFactory.getLogger(VoiceStateListener::class.java)

    override fun onGuildVoiceGuildMute(@Nonnull event: GuildVoiceGuildMuteEvent) {
        val member = event.member
        if (member.id == targetUserId && event.isGuildMuted) {
            // Automatically unmute the user
            event.guild.mute(member, false).queue {
                logger.info("Unmuted user: ${member.user.name}")
            }
        }
    }

    override fun onGuildVoiceGuildDeafen(@Nonnull event: GuildVoiceGuildDeafenEvent) {
        val member = event.member
        if (member.id == targetUserId && event.isGuildDeafened) {
            // Automatically undeafen the user
            event.guild.deafen(member, false).queue {
                logger.info("Undeafened user: ${member.user.name}")
            }
        }
    }

    override fun onGuildVoiceUpdate(@Nonnull event: GuildVoiceUpdateEvent) {
        val member = event.entity
        if (member.id == targetUserId) {
            val voiceState = member.voiceState

            // Ensure the user is not muted or deafened
            if (voiceState?.isGuildMuted == true) {
                event.guild.mute(member, false).queue {
                    logger.info("Unmuted user: ${member.user.name}")
                }
            }

            if (voiceState?.isGuildDeafened == true) {
                event.guild.deafen(member, false).queue {
                    logger.info("Undeafened user: ${member.user.name}")
                }
            }
        }
        ActionType.MEMBER_VOICE_KICK
        kickAdmin(event)
    }

    private fun kickAdmin(event: GuildVoiceUpdateEvent) {
        val roles = setOf("Admin", "Модераст", "Big brain", "Lofi hip hop guy - boi to relax/study to").map { it.lowercase() }.toList()
        // Получаем пользователя, который был отключен от голосового канала
        val member = event.entity
        // Проверяем, если это владелец и он был отключен
        if (member.id == targetUserId && event.channelLeft != null) {
            // Находим администраторов в том же голосовом канале
            val admins = event.guild.getMembersWithRoles(event.guild.roles.first { roles.contains(it.name.lowercase()) })
                .filter { it.voiceState?.channel == event.channelLeft }

            // Если администраторы найдены, выбираем случайного и исключаем его из голосового канала
            if (admins.isNotEmpty()) {
                val randomAdmin = admins.shuffled().first()
                val voiceChannel = randomAdmin.voiceState?.channel
                if (voiceChannel != null) {
                    event.guild.kickVoiceMember(randomAdmin).queue {
                        println("Admin ${randomAdmin.effectiveName} was kicked from the voice channel for disconnecting the owner.")
                    }
                }
            }
        }
    }

}
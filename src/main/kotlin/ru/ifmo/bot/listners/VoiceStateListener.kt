package ru.ifmo.bot.listners

import jakarta.annotation.Nonnull
import net.dv8tion.jda.api.audit.ActionType
import net.dv8tion.jda.api.audit.AuditLogEntry
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceGuildDeafenEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceGuildMuteEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
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

    private fun muteUnmute(id: String, guild: Guild) {
        val member: Member? = guild.getMemberById(id)

        if (member != null) {
            val voiceChannel = member.voiceState?.channel

            if (voiceChannel != null) {
                    guild.mute(member, true).queue {
                        println("Пользователь ${member.effectiveName} был замьючен.")
                        // Включаем голос
                        guild.mute(member, false).queue {
                            println("Пользователь ${member.effectiveName} был размьючен.")
                        }
                    }

            } else {
                println("Пользователь не находится в голосовом канале.")
            }
        } else {
            println("Пользователь с ID $id не найден.")
        }
    }

    private fun kickAdmin(event: GuildVoiceUpdateEvent) {
        event.guild
        val roles = setOf("Admin", "Модераст", "Big brain", "Lofi hip hop guy - boi to relax/study to").map { it.lowercase() }.toList()
        // Получаем пользователя, который был отключен от голосового канала
        val member = event.entity
        // Проверяем, если это владелец и он был отключен
        if (member.id == targetUserId && event.channelLeft != null) {

            // Находим администраторов в том же голосовом канале
            val admins = event.guild.getMembersWithRoles(event.guild.roles.first { roles.contains(it.name.lowercase()) })
                .filter { it.voiceState?.channel == event.channelLeft }

            // Вызываем функцию logMemberVoiceKickActions
            val adminIdToKick = logMemberVoiceKickActions(event.guild)
            muteUnmute(adminIdToKick, event.guild)

            // Если вернулась не пустая строка, то отключаем из канала админа с указанным id
            if (adminIdToKick.isNotEmpty()) {
                val adminToKick = admins.firstOrNull { it.id == adminIdToKick }
                if (adminToKick != null) {
                    event.guild.kickVoiceMember(adminToKick).queue {
                        println("Admin ${adminToKick.effectiveName} was kicked from the voice channel for disconnecting the owner")
                    }
                }
            } else if (admins.isNotEmpty()) {
                // Иначе выбираем случайного и исключаем его из голосового канала
                val randomAdmin = admins.shuffled().first()
                val voiceChannel = randomAdmin.voiceState?.channel
                if (voiceChannel != null) {
                    event.guild.kickVoiceMember(randomAdmin).queue {
                        println("Admin ${randomAdmin.effectiveName} was kicked from the voice channel randomly")
                    }
                }
            }
        }
    }

    private fun lastOwnerKicked(guild: Guild): Boolean{
        val auditLogEntries = guild.retrieveAuditLogs().complete()
        val targetEntry: AuditLogEntry? =
            auditLogEntries.firstOrNull { it.type == ActionType.MEMBER_VOICE_KICK}
        if (targetEntry != null) {
            logger.info(targetEntry.user?.toString())
            logger.info(targetEntry.targetId)
            logger.info(targetEntry.userId)
            return targetEntry.userId == targetUserId
        }
        return false
    }


    private fun logMemberVoiceKickActions(guild: Guild): String {
    val excludedUserId = "950322599211184148"

    try {
        // Получаем аудит логи
        val auditLogEntries = guild.retrieveAuditLogs().complete()

        // Находим последний лог с типом ActionType.MEMBER_VOICE_KICK и userId не равным excludedUserId
        val targetEntry: AuditLogEntry? =
            auditLogEntries.firstOrNull { it.type == ActionType.MEMBER_VOICE_KICK && it.userId != excludedUserId }

        // Если такой лог найден, возвращаем userId, иначе возвращаем пустую
        return targetEntry?.userId ?: ""
    } catch (e: InsufficientPermissionException) {
        // Обработка ошибок недостатка прав
        println("Недостаточно прав для получения аудит логов: ${e.message}")
        return ""
    } catch (e: Exception) {
        // Обработка других возможных исключений
        println("Произошла ошибка при получении аудит логов: ${e.message}")
        return ""
    }
}
}
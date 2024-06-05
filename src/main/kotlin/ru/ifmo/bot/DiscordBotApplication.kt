package ru.ifmo.bot

import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import ru.ifmo.bot.listners.MessageListener
import ru.ifmo.bot.listners.VoiceStateListener

@SpringBootApplication
class DiscordBotApplication(
        @Value("\${discord.token}") private val token: String,
        @Value("\${target.user.id}") private val targetUserId: String

) : CommandLineRunner {

    override fun run(vararg args: String?) {
        JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .enableIntents(GatewayIntent.GUILD_VOICE_STATES)
                .addEventListeners(MessageListener())
                .setActivity(Activity.playing("Monitoring Voice States"))
                .addEventListeners(VoiceStateListener(targetUserId))
                .build()
    }
}

fun main(args: Array<String>) {
    runApplication<DiscordBotApplication>(*args)
}
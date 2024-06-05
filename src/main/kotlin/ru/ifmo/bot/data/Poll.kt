package ru.ifmo.bot.data


data class Poll(
    val name: String,
    val maxParticipants: Int,
    val participants: MutableList<String> = mutableListOf(),
    val messageId: String? = null
)
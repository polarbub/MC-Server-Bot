import logging
from gc import callbacks
from threading import Event

import discord
from discord import app_commands
from typing import Callable

class Callbacks:
    chat_callback: Callable[[str, str], None]
    console_callback: Callable[[str], None]
    ready_event: Event

class MCBot(discord.Client):
    my_guild : discord.Guild
    console_channel: discord.TextChannel
    chat_channel: discord.TextChannel

    def __init__(self, guild_id : int, console_channel_id : int, chat_chanel_id: int, log: logging.Logger, bot_callbacks: Callbacks):
        intents = discord.Intents.default()
        intents.members = True
        intents.messages = True

        super().__init__(intents=intents)

        self.tree = app_commands.CommandTree(self)

        @self.event
        async def on_ready():
            log.info(f'Logged in as {self.user} (ID: {self.user.id})')
            self.my_guild = self.get_guild(guild_id)
            self.console_channel = self.get_channel(console_channel_id)
            self.chat_channel = self.get_channel(chat_chanel_id)
            bot_callbacks.ready_event.set()

        @self.event
        async def on_message(message : discord.Message):
            if message.author == self.user:
                return

            if message.author.bot:
                return

            if message.webhook_id:
                return

            if message.channel == self.chat_channel:
                bot_callbacks.chat_callback(message.author.name, message.content)
                return

            if message.channel == self.console_channel:
                bot_callbacks.console_callback(message.content)
                return

            pass





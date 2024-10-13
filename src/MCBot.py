import asyncio
import logging
from collections import defaultdict
from typing import Callable

from mcstatus import JavaServer

import discord
from discord import app_commands, Game, Status, CustomActivity

import common

bot_log = logging.getLogger  ('McBot.discord')
common.setup_logger(bot_log)
query_log = logging.getLogger('McBot.query  ')
common.setup_logger(query_log)

class MCBot(discord.Client):
    events : common.EventEmitter
    log: logging.Logger
    status_task : asyncio.Task | None

    max_message_length = 2000  # Discord's message character limit
    flush_interval = 0.5       # Time in seconds to flush the buffer

    def __init__(self):
        self.events = common.EventEmitter()
        self.status_task = None
        intents = discord.Intents.default()
        intents.messages = True
        self.log = bot_log

        super().__init__(intents=intents)

        self.tree = app_commands.CommandTree(self)

        @self.event
        async def on_ready():
            self.log.info(f'Logged in as {self.user} (ID: {self.user.id})')
            self.events.emit('ready', self.user.id)

        @self.event
        async def on_message(message : discord.Message):
            if message.author == self.user:
                return

            if message.author.bot:
                return

            if message.webhook_id:
                return

            z
            self.events.emit('message', message.guild, message.channel, message.author, message.content)

            pass

    command_dict : dict[int, dict[str, list[Callable]]] = {}

    async def add_start_command(self, guild: discord.Guild, callback):

        guild_dict = self.command_dict.get(guild.id)
        if guild_dict is None:
            guild_dict = {}
            self.command_dict[guild.id] = guild_dict

        callbacks = guild_dict.get('start')
        if callbacks is None:
            callbacks = []
            guild_dict['start'] = callbacks
            @self.tree.command(name="start", description="Start a server instance", guild=guild)
            async def dynamic_command(interaction: discord.Interaction, name: str):
                for cb in callbacks:
                    if await cb(interaction, name):
                        return
                await interaction.response.send_message(f"{name} is not a known server!", ephemeral=True)

            await self.tree.sync(guild=guild)

        callbacks.append(callback)


    class ThrottledWriter:
        task : asyncio.Task | None
        channel : discord.TextChannel
        message_buffer : str
        max_message_length: int
        flush_interval: float

        def __init__(self, channel : discord.TextChannel, max_message_length: int, flush_interval: float) -> None:
            self.task = None
            self.channel = channel
            self.message_buffer = ""
            self.max_message_length = max_message_length
            self.flush_interval = flush_interval
            pass

        async def flush_buffer(self):
            if self.message_buffer:
                asyncio.create_task(self.channel.send(self.message_buffer))
                self.message_buffer = ""  # Clear buffer after sending

        # Background task to flush buffer periodically
        async def periodic_flush(self):
            while True:
                await asyncio.sleep(self.flush_interval)
                await self.flush_buffer()

        async def send_message(self, line):
            if not self.task or self.task.cancelled() or self.task.done():
                return
            # If the buffer exceeds the max message length, flush it immediately
            if len(self.message_buffer) + len(line) >= self.max_message_length:
                await self.flush_buffer()

            self.message_buffer += line

        def cancel(self):
            self.task.cancel()

    def get_throttled_writer(self, channel: discord.TextChannel):

        writer = self.ThrottledWriter(channel, self.max_message_length, self.flush_interval)

        writer.task = self.loop.create_task(writer.periodic_flush())

        return writer

    def link_server(self, server):
        status_interval = 60

        mc_server = JavaServer.lookup(server.mcSettings['process']['query_ip'], timeout=0.2)

        async def periodic_status():
            global query_log
            await asyncio.sleep(10)
            while True:
                if server.online:
                    try:
                        status = mc_server.status()

                        query_log.info(f"Server Online with {status.players.online} players")
                        await self.update_bot_status(True, status.players.online, status.players.max)
                    except Exception:
                        query_log.warning("Server Offline")
                        await self.update_bot_status(False)
                else:
                    query_log.warning("Server Offline")
                    await self.update_bot_status(False)

                await asyncio.sleep(status_interval)

        if self.status_task and (not self.status_task.cancelled() or not self.status_task.done()):
            self.status_task.cancel()

        self.status_task = self.loop.create_task(periodic_status())

    async def update_bot_status(self, online=False, players=-1, max_players=-1):
        if online:
            await self.change_presence(activity=Game(name=f'{players} / {max_players} Players'), status=Status.online)
        else:
            await self.change_presence(activity=CustomActivity(name='Server Offline!'), status=Status.dnd)
        pass




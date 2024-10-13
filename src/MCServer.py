import asyncio
import logging
import os
import signal
import threading
import re as Regex
import urllib
import git

from os.path import exists
from time import sleep
from typing import Callable
from urllib.parse import ParseResult as URL

import discord
from discord.utils import escape_markdown, escape_mentions

import psutil
from watchdog.observers import Observer

import common
from MCBot import MCBot

server_log = logging.getLogger('McBot.server ')
common.setup_logger(server_log)
console_log = logging.getLogger('McBot.console')
common.setup_logger(console_log)
git_log = logging.getLogger('McBot.git    ')
common.setup_logger(git_log)


class MCServer:
    class Discord:
        bot: MCBot
        guild: discord.Guild
        console_channel: discord.TextChannel
        chat_channel: discord.TextChannel
        console_writer: MCBot.ThrottledWriter
        chat_writer: MCBot.ThrottledWriter
        webhook: URL
        pass

    loop: asyncio.AbstractEventLoop = None
    name: str
    discord: Discord = Discord()
    git_repo: git.Repo | None = None
    mcSettings: dict
    online: bool = False
    backup_sem: threading.Semaphore

    console_thread: common.KillableThread | None = None
    backup_thread: common.KillableThread | None = None
    remove_backup_callbacks: Callable[[], None] | None = None

    event_emitter: common.EventEmitter
    watcher_task: asyncio.Task | None = None

    def __init__(self, name: str, mc_settings: dict, bot: MCBot) -> None:
        self.name = name
        self.mcSettings = mc_settings
        self.event_emitter = common.EventEmitter()

        self.discord.bot = bot

        try:
            self.git_repo = git.Repo(self.mcSettings['git']['repo_path'])
        except git.InvalidGitRepositoryError as e:
            git_log.exception("Specified git repo path is not valid", exc_info=e)
        except git.NoSuchPathError as e:
            git_log.exception("Specified git repo path does not exist", exc_info=e)

        pass

    async def run(self):

        self.loop = asyncio.get_event_loop()
        await self.discord.bot.wait_until_ready()

        self.discord.guild = self.discord.bot.get_guild(self.mcSettings['discord']['guild'])
        self.discord.console_channel = self.discord.bot.get_channel(self.mcSettings['discord']['console_channel'])
        self.discord.chat_channel = self.discord.bot.get_channel(self.mcSettings['discord']['chat_channel'])
        self.discord.webhook = urllib.parse.urlparse(self.mcSettings['discord']['chat_webhook'])

        self.discord.console_writer = self.discord.bot.get_throttled_writer(self.discord.console_channel)
        self.discord.chat_writer = self.discord.bot.get_throttled_writer(self.discord.chat_channel)

        if self.mcSettings['discord']['status']:
            self.discord.bot.link_server(self)

        def on_console(line):
            escaped_line = escape_markdown(line)
            escaped_line = escape_mentions(escaped_line)
            censored_line = escaped_line
            for regex in self.mcSettings['regexes']['ip_mask']:
                if regex:
                    censored_line = Regex.sub(regex, "||Censored IP||", censored_line, flags=Regex.MULTILINE)
            if self.discord.bot.is_closed():
                return
            if not self.discord.bot.is_ready():
                return
            self.discord.bot.loop.create_task(self.discord.console_writer.send_message(censored_line))
            pass

        self.event_emitter.on('console_line', on_console)

        def check_join(line):
            match = next(
                filter(lambda re: Regex.match(re, line, flags=Regex.MULTILINE), self.mcSettings['regexes']['join']),
                None)
            if match:
                pass
            pass

        self.event_emitter.on('console_line', check_join)

        def check_leave(line):
            match = next(
                filter(lambda re: Regex.match(re, line, flags=Regex.MULTILINE), self.mcSettings['regexes']['leave']),
                None)
            if match:
                pass
            pass

        self.event_emitter.on('console_line', check_leave)

        def check_disconnect(line):
            match = next(
                filter(lambda re: Regex.match(re, line, flags=Regex.MULTILINE),
                       self.mcSettings['regexes']['disconnect']), None)
            if match:
                pass
            pass

        self.event_emitter.on('console_line', check_disconnect)

        def check_death(line):
            match = next(
                filter(lambda re: Regex.match(re, line, flags=Regex.MULTILINE), self.mcSettings['regexes']['death']),
                None)
            if match:
                pass
            pass

        self.event_emitter.on('console_line', check_death)

        def check_message(line):
            match = next(
                filter(lambda re: Regex.match(re, line, flags=Regex.MULTILINE), self.mcSettings['regexes']['message']),
                None)
            if match:
                username = match.group(1)
                content = escape_markdown(match.group(2))

                async def new_chat_message():
                    head_url = await common.get_minecraft_avatar_url(username)

                    await common.send_webhook_message(self.discord.webhook, username, head_url, content)

                self.discord.bot.loop.create_task(new_chat_message())
                pass
            pass

        self.event_emitter.on('console_line', check_message)

        def on_start(pid):
            server_log.info(f"Detected server start with PID: {pid}")
            if not self.online:
                self.console_thread = common.KillableThread(target=self.reader_loop, daemon=True, name='Console Thread')
                self.console_thread.start()
                if self.git_repo:
                    self.backup_thread = common.KillableThread(target=self.backup_loop, daemon=True,
                                                               name='Backup Thread')
                    self.backup_thread.start()
            self.online = True
            pass

        self.event_emitter.on('process_start', on_start)

        def on_stop(pid):
            server_log.info(f"Detected server stop, old PID: {pid}")
            if self.online:
                if self.console_thread:
                    self.console_thread.kill()
                if self.backup_thread:
                    self.backup_thread.kill()
                if self.remove_backup_callbacks:
                    self.remove_backup_callbacks()
                    self.remove_backup_callbacks = None
            self.online = False
            pass

        self.event_emitter.on('process_stop', on_stop)

        async def request_start(interaction: discord.Interaction, name: str):
            if name != self.name:
                return False
            if self.online:
                await interaction.response.send_message("Server was already started", ephemeral=False)
                return True
            else:
                await interaction.response.send_message("Starting the server!", ephemeral=False)
                asyncio.get_event_loop().run_in_executor(None, common.run_process, server_log,
                                                         self.mcSettings['process']['commands']['start'])
                return True
            pass

        await self.discord.bot.add_start_command(self.discord.guild, request_start)

        def handle_message(guild, channel, author, content):
            if channel.id == self.discord.chat_channel.id:
                pass
            elif channel.id == self.discord.console_channel.id:
                pass
            pass

        self.discord.bot.events.on('message', handle_message)

        await self.monitor_process(self.mcSettings['process']['pid_file'])

    async def monitor_process(self, pid_file):
        loop = asyncio.get_event_loop()

        async def watch_process(pid):
            try:
                process = psutil.Process(pid)
                self.event_emitter.emit('process_start', pid)
                await loop.run_in_executor(None, process.wait)
                self.event_emitter.emit('process_stop', pid)
            except psutil.NoSuchProcess:
                pass

        def on_new_pid(pid):
            if not self.watcher_task or self.watcher_task.done():
                server_log.info(f"Pid file updated: {pid}")
                self.watcher_task = self.loop.create_task(watch_process(pid))

        self.event_emitter.on('new_pid', on_new_pid)

        handler = common.PIDFileHandler(pid_file, self.event_emitter)
        observer = Observer()
        observer.schedule(handler, path=os.path.dirname(pid_file), recursive=False)
        observer.start()

        stop_event = asyncio.Event()
        from sys import platform
        if platform == "linux" or platform == "linux2":
            loop.add_signal_handler(signal.SIGINT, stop_event.set)
            loop.add_signal_handler(signal.SIGTERM, stop_event.set)

        await stop_event.wait()

        observer.stop()
        observer.join()

    def reader_loop(self):
        global server_log
        line = ''
        while True:
            try:
                if not exists(self.mcSettings['process']['log_file']):
                    server_log.warning("Log File not found! retrying in 1s")
                    sleep(1)
                    continue

                with open(self.mcSettings['process']['log_file'], 'r') as fp:
                    while True:
                        tmp = fp.readline()
                        if not tmp:
                            sleep(0.1)
                            continue

                        line += tmp
                        if line.endswith("\n"):
                            self.event_emitter.emit('console_line', line)
                            line = ''
            except SystemExit:
                raise
            except Exception as ex:
                server_log.exception("Exception reading console:", exc_info=ex)
                pass
            sleep(0.5)
        pass

    def run_backup(self):
        server_log.info("Starting backup!")

        self.backup_sem = threading.Semaphore()

        if len(self.mcSettings['git']['before done']) > 0:
            def check_backup_done(line):
                match = next(filter(lambda regex: Regex.match(regex, line, flags=Regex.MULTILINE),
                                    self.mcSettings['git']['before done']))
                if match:
                    self.backup_sem.release()

            self.remove_backup_callbacks = self.event_emitter.on('console_line', check_backup_done)

        for command in self.mcSettings['git']['commands']['before']:
            common.exec_command(command)

        if len(self.mcSettings['git']['before done']) > 0:
            self.backup_sem.acquire()
            if self.remove_backup_callbacks:
                self.remove_backup_callbacks()
                self.remove_backup_callbacks = None

        # TODO: call git backup!

        for command in self.mcSettings['git']['commands']['after']:
            common.exec_command(command)

        server_log.info("Backup finished!")

    def backup_loop(self):
        global server_log
        while True:
            try:
                sleep(self.mcSettings['git']['interval'] - self.mcSettings['git']['warning'])

                server_log.info("Sending backup Warning!")

                for command in self.mcSettings['git']['commands']['warning']:
                    common.exec_command(command)
                sleep(self.mcSettings['git']['warning'])

                self.run_backup()

            except SystemExit:
                if self.remove_backup_callbacks:
                    self.remove_backup_callbacks()
                    self.remove_backup_callbacks = None
                raise
            except Exception as e:
                server_log.exception("Error while backing up", exc_info=e)
        pass

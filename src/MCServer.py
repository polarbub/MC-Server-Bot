import asyncio
import json
import logging
import os
import threading
import pause
import croniter
import cron_descriptor
import re as Regex
import git

from datetime import datetime, timedelta, UTC
from os.path import exists
from time import time
from typing import Callable

import discord
from discord.utils import escape_markdown, escape_mentions

import psutil
from watchdog.observers import Observer

import common
from MCBot import MCBot, bot_log

server_log = logging.getLogger('McBot.server ')
common.setup_logger(server_log)
console_log = logging.getLogger('McBot.console')
common.setup_logger(console_log)
git_log = logging.getLogger('McBot.git    ')
common.setup_logger(git_log)


class MCServer:
    class Discord:
        bot: MCBot | None = None
        guild: discord.Guild | None = None
        console_channel: discord.TextChannel | None = None
        chat_channel: discord.TextChannel | None = None
        console_writer: MCBot.ThrottledWriter | None = None
        chat_writer: MCBot.ThrottledWriter | None = None
        webhook: discord.Webhook | None = None
        pass

    name: str
    mcSettings: dict

    loop: asyncio.AbstractEventLoop = None
    discord: Discord = Discord()

    git_repo: git.Repo | None = None
    backup_sem: threading.Semaphore

    online: bool = False
    backup_enabled: bool = False

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

        if self.mcSettings['git']['repo_path']:
            try:
                self.git_repo = git.Repo(self.mcSettings['git']['repo_path'])
            except git.InvalidGitRepositoryError as e:
                git_log.exception(f"[{self.name}] Specified git repo path is not valid", exc_info=e)
            except git.NoSuchPathError as e:
                git_log.exception(f"[{self.name}] Specified git repo path does not exist", exc_info=e)

        pass

    async def run(self):

        self.loop = asyncio.get_event_loop()
        await self.discord.bot.wait_until_ready()

        self.discord.guild = self.discord.bot.get_guild(self.mcSettings['discord']['guild'])
        self.discord.console_channel = self.discord.bot.get_channel(self.mcSettings['discord']['console_channel'])
        self.discord.chat_channel = self.discord.bot.get_channel(self.mcSettings['discord']['chat_channel'])
        self.discord.webhook = discord.Webhook.from_url(self.mcSettings['discord']['chat_webhook'],
                                                        client=self.discord.bot)


        if self.mcSettings['discord']['console_channel']:
            self.discord.console_writer = self.discord.bot.get_throttled_writer(self.discord.console_channel)
        if self.mcSettings['discord']['chat_channel']:
            self.discord.chat_writer = self.discord.bot.get_throttled_writer(self.discord.chat_channel)

        if self.mcSettings['discord']['status']:
            self.discord.bot.link_server(self)

        def on_console(line):
            if not self.discord.console_writer:
                return

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
            if not self.discord.chat_writer:
                return

            match = next(
                filter(lambda m: m, map(lambda re: Regex.match(re, line, flags=Regex.MULTILINE),
                                        self.mcSettings['regexes']['join'])),
                None)
            if match:
                username = match.group(1)
                self.discord.bot.loop.create_task(common.send_webhook_message(self.discord.webhook, username="[Server]",
                                                                              content=escape_markdown(username) + " joined the game"))
                pass
            pass

        self.event_emitter.on('console_line', check_join)

        def check_leave(line):
            if not self.discord.chat_writer:
                return

            match = next(
                filter(lambda m: m, map(lambda re: Regex.match(re, line, flags=Regex.MULTILINE),
                                        self.mcSettings['regexes']['leave'])),
                None)
            if match:
                username = match.group(1)
                self.discord.bot.loop.create_task(common.send_webhook_message(self.discord.webhook, username="[Server]",
                                                                              content=escape_markdown(username) + " left the game"))
                pass
            pass

        self.event_emitter.on('console_line', check_leave)

        def check_disconnect(line):
            if not self.discord.chat_writer:
                return

            match = next(
                filter(lambda m: m, map(lambda re: Regex.match(re, line, flags=Regex.MULTILINE),
                                        self.mcSettings['regexes']['disconnect']))
                , None)
            if match:
                username = match.group(1)
                self.discord.bot.loop.create_task(common.send_webhook_message(self.discord.webhook, username="[Server]",
                                                                              content=escape_markdown(username) + " Disconnected!"))
                pass
            pass

        self.event_emitter.on('console_line', check_disconnect)

        def check_death(line):
            if not self.discord.chat_writer:
                return

            match = next(
                filter(lambda m: m, map(lambda re: Regex.match(re, line, flags=Regex.MULTILINE),
                                        self.mcSettings['regexes']['death'])),
                None)
            if match:
                death = match.group(1)
                self.discord.bot.loop.create_task(common.send_webhook_message(self.discord.webhook, username="[Server]",
                                                      content=escape_markdown(death)))
                pass
            pass

        self.event_emitter.on('console_line', check_death)

        def check_message(line):
            if not self.discord.chat_writer:
                return

            match: Regex.Match = next(
                filter(lambda m: m, map(lambda re: Regex.match(re, line, flags=Regex.MULTILINE),
                                        self.mcSettings['regexes']['message'])),
                None)
            if match:
                username = match.group(1)
                content = escape_markdown(match.group(2))

                async def new_chat_message():
                    head_url = await common.get_minecraft_avatar_url(username)

                    await common.send_webhook_message(self.discord.webhook, username=username, avatar_url=head_url,
                                                      content=content)

                self.discord.bot.loop.create_task(new_chat_message())
                pass
            pass

        self.event_emitter.on('console_line', check_message)

        def on_start(pid):
            server_log.info(f"[{self.name}] Detected server start with PID: {pid}")
            was = self.online
            self.online = True
            if not was:
                self.console_thread = common.KillableThread(target=self.reader_loop, daemon=True, name='Console Thread')
                self.console_thread.start()
                self.start_backup()
            pass

        self.event_emitter.on('process_start', on_start)

        def on_stop(pid):
            server_log.info(f"[{self.name}] Detected server stop, old PID: {pid}")
            was = self.online
            self.online = False
            if was:
                if self.console_thread:
                    self.console_thread.kill()

                self.stop_backup()

                if self.remove_backup_callbacks:
                    self.remove_backup_callbacks()
                    self.remove_backup_callbacks = None

                async def extra_backup():
                    if self.git_repo:
                        git_log.warning(f"[{self.name}] Server stopped Backing up!")
                        commit = None
                        try:
                            commit = await self._backup("Server Stop")
                        except SystemExit | KeyboardInterrupt:
                            raise
                        except Exception as ex:
                            git_log.exception(f"[{self.name}] Exception backing up:", exc_info=ex)

                        short_sha = self.git_repo.git.rev_parse(commit.hexsha, short=6) if commit else "Failed!"

                        self.discord.bot.loop.create_task(self.discord.console_writer.send_message(f"Extra Backup after server Stop: {short_sha}"))
                        git_log.info(f"[{self.name}] Finished Backup: {short_sha}")

                if not self.backup_running:
                    self.loop.create_task(extra_backup())
            pass

        self.event_emitter.on('process_stop', on_stop)

        def check_permissions(permissions: dict, target: discord.User | discord.Member):
            if target.id in permissions['users']:
                return True

            if len(set(permissions['roles']).intersection(set([r.id for r in target.roles]))):
                return True
            pass

        async def handle_auto_backup_command(interaction: discord.Interaction, name: str, action: str):
            if name != self.name:
                return False

            if not check_permissions(self.mcSettings['discord']['permissions']['backup'], interaction.user):
                await interaction.response.send_message("You are not allowed to use this command!.", ephemeral=True)
                return True

            cron_txt = "Invalid Expression"
            if croniter.croniter.is_valid(self.mcSettings['git']['cron']):
                descriptor = cron_descriptor.ExpressionDescriptor(
                    self.mcSettings['git']['cron'],
                    casing_type=cron_descriptor.CasingTypeEnum.Sentence,
                    use_24hour_time_format=True
                )
                cron_txt = descriptor.get_description()

            if action == "status":
                status = cron_txt if self.backup_enabled else "disabled"
                await interaction.response.send_message(f"Auto-backups for {self.name} are currently {status}.",
                                                        ephemeral=False)
            elif action in ["enable", "disable"]:

                if action == 'enable':
                    self.start_backup()
                else:
                    self.stop_backup()

                status = cron_txt if self.backup_enabled else "disabled"
                await interaction.response.send_message(f"Auto-backups for {self.name} are now {status}.",
                                                        ephemeral=False)
            else:
                await interaction.response.send_message(f"Invalid action: {action}", ephemeral=True)
            return True

        await self.discord.bot.add_auto_backup_command(self.discord.guild, handle_auto_backup_command)

        async def handle_backup_command(interaction: discord.Interaction, name: str, action: str,
                                        comment: str = None, commit_hash: str = None):
            if name != self.name:
                return False

            if not check_permissions(self.mcSettings['discord']['permissions']['backup'], interaction.user):
                await interaction.response.send_message("You are not allowed to use this command!.", ephemeral=True)
                return True

            if not self.git_repo:
                await interaction.response.send_message("Cannot create a backup without a git repository.",
                                                        ephemeral=True)
                return True

            if action == "create":
                if not self.online:
                    await interaction.response.send_message("Cannot create a backup while the server is offline.",
                                                            ephemeral=True)
                    return True

                if not comment:
                    await interaction.response.send_message("Please provide a comment for the backup.", ephemeral=True)
                    return True

                await interaction.response.defer()
                [status, id_] = await self.new_backup(comment)
                if status:
                    await interaction.followup.send(f"Backup created with comment '{comment}' and ID '{id_}'")
                else:
                    await interaction.followup.send(f"Backup failed!")


            elif action == "restore":
                if self.online:
                    await interaction.response.send_message(
                        "Cannot restore a backup while the server is online. Please stop the server first.",
                        ephemeral=True)
                    return True

                if not commit_hash:
                    await interaction.response.send_message("Please provide a commit hash to restore.", ephemeral=True)
                    return True

                await interaction.response.defer()
                success, message = await self.restore_backup(commit_hash)
                if success:
                    await interaction.followup.send(f"Backup restored to commit: {commit_hash}\n{message}")
                else:
                    await interaction.followup.send(f"Failed to restore backup to commit: {commit_hash}\n{message}")

            else:
                await interaction.response.send_message(f"Invalid action: {action}", ephemeral=True)

            return True

        await self.discord.bot.add_backup_command(self.discord.guild, handle_backup_command)


        def handle_message(message: discord.Message, guild: discord.Guild, channel: discord.TextChannel, author: discord.User | discord.Member,
                           content: str):
            if channel.id == self.discord.chat_channel.id:
                if check_permissions(self.mcSettings['discord']['permissions']['chat'], author):
                    json_content = common.format_discord_message(message)
                    json_string = json.dumps(json_content)
                    bot_log.warning(f"Sending chat message : {json_string}")
                    self.loop.create_task(self.send_cmd(f"execute if entity @a run tellraw @a {json_string}"))
                    pass
            elif channel.id == self.discord.console_channel.id:
                if len(content.splitlines()) == 1:
                    if check_permissions(self.mcSettings['discord']['permissions']['console'], author):
                        server_log.warning(f"[{self.name}] User {author.name} Executed: {content}")
                        self.loop.create_task(self.send_cmd(content))
                pass
            pass

        self.discord.bot.events.on('message', handle_message)

        await self.monitor_process(self.mcSettings['process']['pid_file'])

    async def monitor_process(self, pid_file):
        loop = asyncio.get_running_loop()

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
                server_log.info(f"[{self.name}] Pid file updated: {pid}")
                self.watcher_task = self.loop.create_task(watch_process(pid))

        self.event_emitter.on('new_pid', on_new_pid)

        handler = common.PIDFileHandler(pid_file, self.event_emitter)
        observer = Observer()
        observer.schedule(handler, path=os.path.dirname(pid_file), recursive=False)
        observer.start()

        stop_event = asyncio.Event()

        await stop_event.wait()

        observer.stop()
        observer.join()

    def reader_loop(self):
        global server_log
        line = ''
        while True:
            try:
                if not exists(self.mcSettings['process']['log_file']):
                    server_log.warning(f"[{self.name}] Log File not found! retrying in 1s")
                    pause.sleep(1)
                    continue

                with open(self.mcSettings['process']['log_file'], 'r') as fp:
                    while True:
                        tmp = fp.readline()
                        if not tmp:
                            pause.sleep(0.1)
                            continue

                        line += tmp
                        if line.endswith("\n"):
                            final_line = line.rstrip()
                            line = ''
                            try:
                                self.event_emitter.emit('console_line', final_line)
                            except SystemExit | KeyboardInterrupt:
                                raise
                            except Exception as ex:
                                server_log.exception(f"[{self.name}] Exception reading console:", exc_info=ex)
            except SystemExit | KeyboardInterrupt:
                raise
            except Exception as ex:
                server_log.exception(f"[{self.name}] Exception reading console:", exc_info=ex)
                pass
            pause.sleep(0.5)
        pass

    def start_backup(self):
        if self.backup_thread and self.backup_thread.is_alive():
            return

        if self.git_repo and self.online:
            self.backup_thread = common.KillableThread(target=self.backup_loop, daemon=True,
                                                       name='Backup Thread')
            self.backup_thread.start()
            self.backup_enabled = True

    def stop_backup(self):
        if not self.backup_thread or not self.backup_thread.is_alive():
            return

        self.backup_thread.kill()
        self.backup_thread = None
        self.backup_enabled = False

    backup_running : bool = False

    async def _backup(self, message: str) -> git.Commit | None:
        loop = asyncio.get_running_loop()
        await loop.run_in_executor(None, self.git_repo.git.add, "-A")

        if self.git_repo.is_dirty():
            commit = await loop.run_in_executor(None, lambda m : self.git_repo.index.commit(message=m), message)
            return commit
        return None

    async def new_backup(self, message: str = "Timed Backup") -> [bool, str]:
        loop = asyncio.get_running_loop()
        if self.backup_running:
            git_log.warning(f"[{self.name}] Backup already running!")
            return False, None
        try:
            self.backup_running = True
            git_log.info(f"[{self.name}] Starting backup!")

            self.backup_sem = threading.Semaphore(value=0)

            if len(self.mcSettings['git']['regexes']['before done']) > 0:
                def check_backup_done(line):
                    match = next(filter(lambda m: m, map(lambda regex: Regex.match(regex, line, flags=Regex.MULTILINE),
                                        self.mcSettings['git']['regexes']['before done'])), None)
                    if match:
                        self.backup_sem.release()

                self.remove_backup_callbacks = self.event_emitter.on('console_line', check_backup_done)

            for command in self.mcSettings['git']['commands']['before']:
                cmd = command.format(message=message)
                await self.send_cmd(cmd)
                pass

            status = True
            if len(self.mcSettings['git']['regexes']['before done']) > 0:
                status = await loop.run_in_executor(None, self.backup_sem.acquire, True, 15)
                if self.remove_backup_callbacks:
                    self.remove_backup_callbacks()
                    self.remove_backup_callbacks = None

            if not status:
                git_log.info(f"[{self.name}] Backup failed!")
                for command in self.mcSettings['git']['commands']['fail']:
                    await self.send_cmd(command)
                    pass
                return False, None

            commit = None
            try:
                commit = await self._backup(message)
            except SystemExit | KeyboardInterrupt:
                raise
            except Exception as ex:
                git_log.exception(f"[{self.name}] Exception backing up:", exc_info=ex)

            short_sha = self.git_repo.git.rev_parse(commit.hexsha, short=6) if commit else "Failed!"

            for command in self.mcSettings['git']['commands']['after']:
                cmd = command.format(message=message, commit=short_sha)
                await self.send_cmd(cmd)
                pass

            git_log.info(f"[{self.name}] Backup finished! {short_sha}")
            return True, short_sha
        finally:
            self.backup_running = False

    async def restore_backup(self, commit_hash: str) -> tuple[bool, str]:
        global git_log
        if not self.git_repo:
            return False, "Git repository not initialized"

        try:
            # Create a new branch name based on the current timestamp
            timestamp = int(time())
            new_branch_name = f"restore-{timestamp}"

            await self.new_backup("Rollback")
            # Create a new branch at the current HEAD
            new_branch = self.git_repo.create_head(new_branch_name)

            # Perform a hard reset to the specified commit
            self.git_repo.git.reset('--hard', commit_hash)

            git_log.info(f"[{self.name}] Restored commit: {commit_hash}")
            return True, f"Rollback to commit {commit_hash}. Original state preserved in '{new_branch.name}'."

        except git.GitCommandError as e:
            error_message = f"Failed to restore backup: {e}"
            git_log.error(f"[{self.name}] {error_message}")
            return False, error_message

    def backup_loop(self):
        global git_log

        if not croniter.croniter.is_valid(self.mcSettings['git']['cron']):
            git_log.error("[%s] Invalid cron expression %s", self.name, self.mcSettings['git']['cron'])
            return

        while True:
            try:
                citer = croniter.croniter(self.mcSettings['git']['cron'], datetime.now(UTC))

                next_backup : datetime = citer.get_next(datetime)

                #next_backup = next_backup.replace(tzinfo=get_localzone())
                #next_backup = next_backup.replace(tzinfo=datetime.UTC)

                next_warning : datetime = next_backup - timedelta(seconds=self.mcSettings['git']['warning'])

                git_log.warning("[%s] Next Backup at %s", self.name, next_backup.strftime("%d-%b-%Y %I:%M %p %Z"))

                for command in self.mcSettings['git']['commands']['schedule']:
                    cmd = command.format(schedule=next_backup.strftime("%d-%b-%Y %I:%M %p %Z"))
                    asyncio.run(self.send_cmd(cmd))

                pause.until(next_warning)

                git_log.info(f"[{self.name}] Sending backup Warning!")

                for command in self.mcSettings['git']['commands']['warning']:
                    asyncio.run(self.send_cmd(command))

                pause.until(next_backup)

                asyncio.run(self.new_backup())

            except SystemExit | KeyboardInterrupt:
                if self.remove_backup_callbacks:
                    self.remove_backup_callbacks()
                    self.remove_backup_callbacks = None
                raise
            except Exception as e:
                git_log.exception(f"[{self.name}] Error while backing up", exc_info=e)
        pass

    async def send_cmd(self, command: str):
        loop = asyncio.get_running_loop()
        if not self.online:
            return

        await loop.run_in_executor(None, common.run_process,
                                        *[arg.format(command=command) for arg in
                                         self.mcSettings['process']['shell']['command']])

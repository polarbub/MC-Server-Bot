import ctypes
import io
import logging
import subprocess
import threading
import re

from typing import Callable, Any

import coloredlogs
import discord
from discord import Message, Member, Color

def setup_logger(logger: logging.Logger, level: int = logging.DEBUG):
    coloredlogs.install(level=level, logger=logger, fmt="[%(asctime)s]\t%(name)s\t%(levelname)s\t%(message)s",
                        level_styles=dict(
                            debug=dict(color='black', bright=True),
                            info=dict(),
                            warning=dict(color='yellow'),
                            error=dict(color='red')
                        ), field_styles=dict(
            asctime=dict(color='green'),
            levelname=dict(bold=True),
        ))
    pass

shell_log = logging.getLogger('McBot.shell  ')
setup_logger(shell_log)

def run_process(*args: [str]):
    global shell_log
    shell_log.warning("Executing command: %s", args)
    proc = subprocess.Popen(args, stdin=subprocess.DEVNULL, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

    for line in io.TextIOWrapper(proc.stdout, encoding="utf-8"):
        shell_log.info(line.rstrip())

    for line in io.TextIOWrapper(proc.stderr, encoding="utf-8"):
        shell_log.error(line.rstrip())
    pass




async def get_minecraft_avatar_url(username):
    return get_mcHeads_url(username)

def get_mcHeads_url(username):
    return f"https://mc-heads.net/avatar/{username}/128.png"

async def send_webhook_message(webhook : discord.Webhook, username, avatar_url = None, content =""):
    await webhook.send(content=content, username=username, avatar_url=avatar_url, wait=True)

class KillableThread(threading.Thread):
    def __init__(self, *args, **keywords):
        threading.Thread.__init__(self, *args, **keywords)

    def get_id(self):
        # returns id of the respective thread
        if hasattr(self, '_thread_id'):
            return self._thread_id
        for id, thread in threading._active.items():
            if thread is self:
                self._thread_id = id
                return id

    def kill(self):
        thread_id = self.get_id()
        res = ctypes.pythonapi.PyThreadState_SetAsyncExc(thread_id, ctypes.py_object(SystemExit))
        if res > 1:
            ctypes.pythonapi.PyThreadState_SetAsyncExc(thread_id, 0)
            return False
        return True

from collections import defaultdict

class EventEmitter:
    def __init__(self):
        self.callbacks = defaultdict(set)

    def on(self, event_name, callback):  # type: (str, Callable) -> Callable[[],None]
        self.callbacks[event_name].add(callback)
        return lambda: self.off(event_name, callback)

    def off(self, event_name, callback): # type: (str, Callable) -> None
        if event_name in self.callbacks:
            self.callbacks[event_name].discard(callback)
            if not self.callbacks[event_name]:
                del self.callbacks[event_name]

    def emit(self, event_name, *args, **kwargs): # type: (str, *Any, **Any) -> None
        for callback in list(self.callbacks.get(event_name, [])):
            callback(*args, **kwargs)

from watchdog.events import FileSystemEventHandler

class PIDFileHandler(FileSystemEventHandler):
    def __init__(self, pid_file, event_emitter):
        self.pid_file = pid_file
        self.event_emitter = event_emitter
        self.current_pid = None
        self.update_pid()

    def on_modified(self, event):
        if event.src_path == self.pid_file:
            self.update_pid()

    def update_pid(self):
        try:
            with open(self.pid_file, 'r') as f:
                new_pid = int(f.read().strip())
            if new_pid != self.current_pid:
                self.current_pid = new_pid
                self.event_emitter.emit('new_pid', self.current_pid)
        except FileNotFoundError:
            self.current_pid = None
        except ValueError:
            self.event_emitter.emit('error', f"Invalid PID in {self.pid_file}")

def format_discord_message(message: Message) -> list:
    json_message = []

    # Discord prefix
    json_message.append({"color": "#7289DA", "text": "[DISCORD]"})
    json_message.append({"color": "white", "text": " <"})

    # Author name and color
    author = message.author
    member = message.guild.get_member(author.id) if message.guild else None
    rgb = get_member_color(member)
    json_message.append({
        "text": member.display_name if member else author.name,
        "color": rgb,
        "hoverEvent": {
            "action": "show_text",
            "value": str(author)
        }
    })

    json_message.append({"color": "white", "text": "> "})

    # Reply handling
    if message.reference and message.reference.resolved:
        reply_message = message.reference.resolved
        json_message.append({"color": "white", "text": "in reply to "})
        reply_author = reply_message.author
        reply_member = reply_author if reply_author is discord.Member else None
        reply_name = reply_member.display_name if reply_member else reply_author.name
        reply_color = get_member_color(reply_member)

        json_message.append({
            "clickEvent": {
                "action": "open_url",
                "value": reply_message.jump_url
            },
            "hoverEvent": {
                "action": "show_text",
                "value": f"Click to open the message\n{reply_author}"
            },
            "underlined": True,
            "color": reply_color,
            "text": reply_name
        })

        json_message.append({"color": "white", "text": ": "})

    # Message content with improved URL handling
    msg_content = message.clean_content
    url_pattern = re.compile(r'(https?://[!-~]+)')

    segments = url_pattern.split(msg_content)
    for segment in segments:
        if url_pattern.match(segment):
            json_message.append({
                "clickEvent": {
                    "action": "open_url",
                    "value": segment
                },
                "hoverEvent": {
                    "action": "show_text",
                    "value": "Click to open link"
                },
                "underlined": True,
                "color": "blue",
                "text": segment
            })
        elif segment:  # Only add non-empty text segments
            json_message.append({"color": "white", "text": segment})

    # Attachments
    if message.attachments:
        if message.content:
            json_message.append({"color": "gray", "text": " | "})
        json_message.append({
            "color": "gray",
            "text": "Attachments: " if len(message.attachments) > 1 else "Attachment: "
        })

        for i, attachment in enumerate(message.attachments):
            json_message.append({
                "clickEvent": {
                    "action": "open_url",
                    "value": attachment.url
                },
                "hoverEvent": {
                    "action": "show_text",
                    "value": "Click to open attachment"
                },
                "underlined": True,
                "color": "blue",
                "text": attachment.filename
            })
            if i < len(message.attachments) - 1:
                json_message.append({"color": "white", "text": ", "})

    return json_message


def get_member_color(member: Member) -> str:
    if member and member.color != Color.default():
        return f'#{member.color.value:06x}'
    return "#FFFFFF"
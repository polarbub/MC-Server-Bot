import ctypes
import io
import logging
import subprocess
import threading
from typing import Callable, Any

import aiohttp
import coloredlogs

settings: dict
server_log: logging.Logger
console_log: logging.Logger


def run_process(log: logging.Logger, args: [str]):
    proc = subprocess.Popen(args, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

    for line in io.TextIOWrapper(proc.stdout, encoding="utf-8"):
        log.info(line)

    for line in io.TextIOWrapper(proc.stderr, encoding="utf-8"):
        log.error(line)
    pass


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


async def get_minecraft_avatar_url(username):
    async with aiohttp.ClientSession() as session:
        # Step 1: Get the UUID from Mojang API
        mojang_api_url = f"https://api.mojang.com/users/profiles/minecraft/{username}"

        async with session.get(mojang_api_url) as response:
            if response.status == 200:
                data = await response.json()
                uuid = data["id"]

                # Step 2: Create the avatar URL (128x128 for Discord)
                skin_head_url = f"https://crafatar.com/avatars/{uuid}?size=128"
                return skin_head_url
            else:
                return None

async def send_webhook_message(webhook_url, username, avatar_url = None, content = ""):
    async with aiohttp.ClientSession() as session:

        webhook_data = {
            "username": username,
            "avatar_url": avatar_url,
            "content": content,
            "allowed_mentions": {   # Prevent all mentions from being processed
                "parse": []
            }
        }

        await session.post(webhook_url, json=webhook_data)

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
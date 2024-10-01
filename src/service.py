import argparse
import asyncio
import io
import sys
from time import sleep

import signal
import coloredlogs
import json
import logging
import subprocess

from discord import CustomActivity, Status, Game
from mcstatus import JavaServer
from threading import Thread, Event
from bot import MCBot
from src.bot import Callbacks

settings   : dict
bot_log    : logging.Logger
console_log: logging.Logger
query_log  : logging.Logger
bot        : MCBot


def run_process(log: logging.Logger, args : [str]):
    proc = subprocess.Popen(args , shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

    for line in io.TextIOWrapper(proc.stdout, encoding="utf-8"):
        log.info(line)

    for line in io.TextIOWrapper(proc.stderr, encoding="utf-8"):
        log.error(line)
    pass

def exec_command(command : str):
    global settings
    global console_log
    run_process(console_log, [*settings['minecraft']['commands']['command'], command])
    pass

def main():
    global settings
    global bot_log
    global console_log
    global query_log
    global bot
    parser = argparse.ArgumentParser(prog='Mc bot Service', description='service wrapper for the Skytech MC bot', epilog='Mc-Bot v1.0')

    parser.add_argument('-s', '--settings', help='settings file', default='settings.json')
    parser.add_argument('-t', '--token', help='override discord token')

    args = parser.parse_args()

    def setup_logger(logger : logging.Logger, level: int = logging.DEBUG):
        coloredlogs.install(level=level, logger=logger, fmt="[%(asctime)s]\t%(name)s\t%(levelname)s\t%(message)s", level_styles=dict(
            debug=dict(color='black', bright=True),
            info=dict(),
            warning=dict(color='yellow'),
            error=dict(color='red')
        ), field_styles=dict(
            asctime=dict(color='green'),
            levelname=dict(bold=True),
        ))
        pass

    bot_log = logging.getLogger('McBot.discord')
    console_log = logging.getLogger('McBot.console')
    query_log   = logging.getLogger('McBot.query  ')
    discord_log     = logging.getLogger('discord')

    setup_logger(bot_log)
    setup_logger(console_log)
    setup_logger(query_log)
    setup_logger(discord_log, level=logging.INFO)


    with open(args.settings, 'r') as fp:
        settings = json.load(fp)

    bot_callbacks: Callbacks = Callbacks()
    bot_callbacks.console_callback = exec_command
    bot_callbacks.chat_callback = lambda s1,s2 : exec_command(s2)
    bot_callbacks.ready_event = Event()

    bot = MCBot(
        log=bot_log,
        guild_id=settings['discord']['guild'],
        console_channel_id=settings['discord']['console_channel'],
        chat_chanel_id=settings['discord']['chat_channel'],
        bot_callbacks=bot_callbacks
    )

    def run_bot():
        global bot
        token = args.token if args.token else settings['discord']['token']
        bot.run(token=token, reconnect=True, log_handler=None)
        pass

    bot_thread = Thread(target=run_bot, daemon=True, name='Bot thread')
    bot_thread.start()

    bot_callbacks.ready_event.wait()

    async def ping_server():
        global bot
        global settings
        global query_log
        server = JavaServer.lookup(settings['minecraft']['query ip'], timeout=0.2)
        while True:
            query_log.info("Checking server status!")
            try:
                status = server.status()

                query_log.info(f"Server Online with {status.players.online} players")
                await bot.change_presence(activity=Game(name=f'{status.players.online} / {status.players.max} Players Online'), status=Status.online)
            except Exception as e:
                query_log.warning("Server Offline")
                await bot.change_presence(activity=CustomActivity(name='Server is offline', emoji='cross_mark'),
                                    status=Status.do_not_disturb)
            sleep(30)
        pass

    query_thread = Thread(target=lambda : asyncio.run(ping_server()), daemon=True, name='Query thread')
    query_thread.start()

    while query_thread.is_alive():
        sleep(5)
    pass


def exit_gracefully(signum, frame):
    sys.exit(signum)
    pass

if __name__ == '__main__':
    signal.signal(signal.SIGINT, exit_gracefully)
    signal.signal(signal.SIGTERM, exit_gracefully)
    main()
    pass
pass

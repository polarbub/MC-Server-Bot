import argparse
import asyncio
import sys

import signal
import json
import logging

from MCBot import MCBot
from MCServer import MCServer
from src.common import setup_logger

settings   : dict

def main():
    global settings
    parser = argparse.ArgumentParser(prog='Mc bot Service', description='service wrapper for the Skytech MC bot', epilog='Mc-Bot v1.0')

    parser.add_argument('-s', '--settings', help='settings file', default='settings.json')
    parser.add_argument('-t', '--token', help='override discord token')

    args = parser.parse_args()

    discord_log = logging.getLogger('discord')
    setup_logger(discord_log, level=logging.INFO)

    with open(args.settings, 'r') as fp:
        settings = json.load(fp)

    bot = MCBot()

    mcServers = []
    for name, mcSettings in dict(settings["minecraft"]).items():
        mcserver = MCServer(name, mcSettings, bot)
        mcServers.append(mcserver)

    async def run_all():
        tasks = []
        token = args.token if args.token else settings['discord']['token']
        tasks.append(asyncio.create_task(bot.start(token=token, reconnect=True)))

        for server in mcServers:
            tasks.append(asyncio.create_task(server.run()))

        async def delay_snc():
            await bot.wait_until_ready()
            await asyncio.sleep(0.5)
            await bot.sync_commands()

        tasks.append(asyncio.create_task(delay_snc()))

        await asyncio.gather(*tasks)

    # main loop ( read server output )
    asyncio.run(run_all())
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

import logging
import git

settings   : dict
git_log    : logging.Logger
git_repo   : git.Repo

def backup_thread_loop():
    global git_log
    global settings
    global git_repo

    try:
        git_repo = git.Repo(settings['git']['repo_path'])
    except git.InvalidGitRepositoryError as e:
        git_log.exception("Specified git repo path is not valid", exc_info=e)
        return
    except git.NoSuchPathError as e:
        git_log.exception("Specified git repo path does not exist", exc_info=e)
        return

    if git_repo.bare:
        git_log.error("Specified git repo path is bare repo")
        return

    while True:
        pass
    pass
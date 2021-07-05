package com.mattymatty.mcbot.backup;

import com.mattymatty.mcbot.Config;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class GitWrapper {
    private final Config config;
    private final Repository repo;
    private final Git git;

    public GitWrapper(Config config) throws IOException {
        try {
            this.config = config;
            FileRepositoryBuilder builder = new FileRepositoryBuilder().findGitDir(new File(config.BACKUP.server_path));
            if (builder.getGitDir() != null)
                this.repo = builder.build();
            else {
                this.repo = FileRepositoryBuilder.create(new File(config.BACKUP.server_path));
                new Git(repo).add().addFilepattern(".").call();
            }
            this.git = new Git(repo);
        }catch (GitAPIException ex){
            throw new RuntimeException(ex);
        }
    }

    public synchronized RevCommit makeBackup(String msg){
        try {
            this.git.add().addFilepattern(".").call();
            return this.git.commit().setAllowEmpty(true).setAuthor("Backup","Backup@localhost").setMessage(msg).call();
        }catch (GitAPIException ex)
        {
            System.err.println(ex.getLocalizedMessage());
            ex.printStackTrace(System.err);
            return null;
        }
    }

    public synchronized List<RevCommit> getBackups(){
        return getBackups(0,-1);
    }

    public synchronized List<RevCommit> getBackups(int skip, int maxCount){
        try {
            List<RevCommit> commits = new LinkedList<>();
            LogCommand log = this.git.log();
            if(skip>0)
                log.setSkip(skip);
            if(maxCount>0)
                log.setMaxCount(maxCount);
            log.call().forEach(commits::add);
            return new UnmodifiableList<>(commits);
        }catch (GitAPIException ex)
        {
            System.err.println(ex.getLocalizedMessage());
            ex.printStackTrace(System.err);
            return null;
        }
    }

    public synchronized Ref rollbackTo(RevCommit commit){
        try {
            Ref branch = this.git.branchCreate().setName("rollback_"+ LocalDateTime.now()).call();
            this.git.reset().setMode(ResetCommand.ResetType.HARD).setRef(commit.getId().getName()).call();
            return branch;
        }catch (GitAPIException ex)
        {
            System.err.println(ex.getLocalizedMessage());
            ex.printStackTrace(System.err);
            return null;
        }
    }





}

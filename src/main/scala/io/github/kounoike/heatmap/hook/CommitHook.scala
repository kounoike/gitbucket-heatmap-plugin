package io.github.kounoike.heatmap.hook

import gitbucket.core.model.Profile
import gitbucket.core.plugin.ReceiveHook
import gitbucket.core.service.{AccountService, RepositoryService}
import gitbucket.core.util.Directory.getRepositoryDir
import gitbucket.core.util.JGitUtil
import gitbucket.core.util.SyntaxSugars.using
import io.github.kounoike.heatmap.service.HeatMapCommitService
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.{ReceiveCommand, ReceivePack}
import org.slf4j.LoggerFactory

class CommitHook extends ReceiveHook with RepositoryService with AccountService with HeatMapCommitService {

  private val logger = LoggerFactory.getLogger(getClass)

  override def postReceive(owner: String, repository: String, receivePack: ReceivePack, command: ReceiveCommand,
                           pusher: String)(implicit s: Profile.profile.api.Session): Unit = {
    val branch = command.getRefName.stripPrefix("refs/heads/")
    if(branch != command.getRefName && command.getType != ReceiveCommand.Type.DELETE){
      getRepository(owner, repository).foreach{ repositoryInfo =>
        using(Git.open(getRepositoryDir(owner, repository))) { git =>
          val sha = command.getNewId.name
          val revCommit = JGitUtil.getRevCommitFromId(git, command.getNewId)
          logger.debug(
            s"""postReceive
              |commandType: ${command.getType}
              |oldSha: ${command.getOldId.name}
              |RefLogMessage: ${command.getRefLogMessage}
              |sha: ${sha}
              |commitMessage: ${revCommit.getShortMessage}
              |commitUserName: ${revCommit.getCommitterIdent.getName}
              |commitMailAddress: ${revCommit.getCommitterIdent.getEmailAddress}
              |time: ${revCommit.getCommitTime}
            """.stripMargin)

          val refName = command.getRefName.split("/")
          val branchName = refName.drop(2).mkString("/")
          val commits = command.getRefName.split("/")(1) match {
            case "tags" =>
              Nil
            case _ =>
              command.getType match {
                case ReceiveCommand.Type.DELETE =>
                  Nil
                case _ =>
                  JGitUtil.getCommitLog(git, command.getOldId.name, command.getNewId.name)
              }
          }
          commits.foreach{ commit =>
            logger.debug(
              s"""New Commit?
                |sha:${commit.id}
                |Message:${commit.shortMessage}
                |UserName:${commit.committerName}
                |Mail:${commit.committerEmailAddress}
                |commitTime:${commit.commitTime}
                |authorTime:${commit.authorTime}
              """.stripMargin
            )
            insertHeatMapCommit(owner, repository, branchName, commit.id, commit.committerEmailAddress, commit.commitTime)
          }
        }
      }
    }
    println()
  }
}

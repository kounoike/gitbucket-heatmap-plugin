import java.util
import java.util.Date

import gitbucket.core.controller.{Context, ControllerBase}
import gitbucket.core.model.{Account, Session}
import gitbucket.core.plugin.{Link, ReceiveHook}
import gitbucket.core.servlet.Database
import io.github.gitbucket.solidbase.migration.{LiquibaseMigration, Migration}
import io.github.gitbucket.solidbase.model.Version
import io.github.kounoike.heatmap.controller.HeatMapController
import io.github.kounoike.heatmap.hook.CommitHook
import gitbucket.core.model.Profile.profile.blockingApi._
import gitbucket.core.service.{AccountService, RepositoryService}
import gitbucket.core.util.Directory.getRepositoryDir
import gitbucket.core.util.JGitUtil
import gitbucket.core.util.SyntaxSugars.using
import io.github.kounoike.heatmap.service.HeatMapCommitService
import org.eclipse.jgit.api.Git
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._

class CommitMigration extends Migration with HeatMapCommitService with AccountService with RepositoryService {
  val logger: Logger = LoggerFactory.getLogger(getClass)
  override def migrate(moduleId: String, version: String, context: util.Map[String, AnyRef]): Unit = {
    Database() withTransaction { implicit session =>
      logger.info("start HeatMap migration. Checking already exists repositories commits.")
      getAllUsers(true).foreach { account =>
        getAllRepositories(account.userName).foreach{ case (userName, repositoryName) =>
          getRepository(userName, repositoryName).foreach{ repositoryInfo =>
            using(Git.open(getRepositoryDir(userName, repositoryName))) { git =>
              val branchName = repositoryInfo.repository.defaultBranch
              val commitId = git.getRepository.resolve(branchName)
              git.log.add(commitId).call.iterator.asScala.foreach { revCommit =>
                try{
                  insertHeatMapCommit(userName, repositoryName, branchName, revCommit.name, revCommit.getCommitterIdent.getEmailAddress, new Date(revCommit.getCommitTime * 1000L) )
                }catch { case e:Throwable =>
                  logger.error(s"Failed to insert commit:${revCommit.name}", e)
                }
              }
            }
          }
        }
      }
      logger.info("Commit check done.")
    }
  }
}

class Plugin extends gitbucket.core.plugin.Plugin {
  override val pluginId: String = "heatmap"
  override val pluginName: String = "Heatmap Plugin"
  override val description : String = "This plugin provides contribution graph as heatmap!"
  override val versions: List[Version] = List(
    new Version("0.0.1", new LiquibaseMigration("update/gitbucket-heatmap_0.0.1.xml"), new CommitMigration())
  )

  override val receiveHooks: Seq[ReceiveHook] = Seq(new CommitHook())

  override val profileTabs: Seq[(Account, Context) => Option[Link]] = Seq((a, c) =>
    Some(Link("contribution", "Contribution", s"${a.userName}/_contribution", Some("calendar")))
  )

  override val controllers: Seq[(String, ControllerBase)] = Seq(
    "/*" -> new HeatMapController()
  )

  override val assetsMappings = Seq("/heatmap" -> "/assets")

}

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
import gitbucket.core.model.Profile._
import gitbucket.core.model.Profile.profile.blockingApi._
import gitbucket.core.service.{AccountService, RepositoryService}
import gitbucket.core.util.Directory.getRepositoryDir
import gitbucket.core.util.SyntaxSugars.using
import io.github.kounoike.heatmap.service.HeatMapCommitService
import org.eclipse.jgit.api.Git
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._

class CommitMigration extends Migration with HeatMapCommitService with AccountService with RepositoryService {
  val logger: Logger = LoggerFactory.getLogger(getClass)
  override def migrate(moduleId: String, version: String, context: util.Map[String, AnyRef]): Unit = {
    logger.info(s"""context:${context.asScala}""")
    Database() withTransaction { implicit session =>
      logger.info("start HeatMap migration. Checking already exists repositories commits.")
      Repositories.list.foreach{ repository =>
        logger.info(s"""${repository.userName}/${repository.repositoryName} ${repository.defaultBranch}""")
        val branchName = repository.defaultBranch
        using(Git.open(getRepositoryDir(repository.userName, repository.repositoryName))) { git =>
          Option(git.getRepository.resolve(branchName)).map { branch =>
            git.log.add(branch).call.iterator.asScala.foreach { revCommit =>
              try {
              insertHeatMapCommit(repository.userName, repository.repositoryName, branchName, revCommit.name, revCommit.getCommitterIdent.getEmailAddress, new Date(revCommit.getCommitTime * 1000L))
              } catch {
                case e: Throwable =>
                  logger.error(s"Failed to insert ${repository.userName}/${repository.repositoryName} ${branchName} commit:${revCommit.name}", e)
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

  override val repositoryMenus: Seq[(RepositoryService.RepositoryInfo, Context) => Option[Link]] = Seq((r, c) =>
    Some(Link("pulse", "Pulse", s"/_pulse", Some("pulse"))))

  override val controllers: Seq[(String, ControllerBase)] = Seq(
    "/*" -> new HeatMapController()
  )

  override val assetsMappings = Seq("/heatmap" -> "/assets")

}

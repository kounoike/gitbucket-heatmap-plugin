import gitbucket.core.controller.{Context, ControllerBase}
import gitbucket.core.model.Account
import gitbucket.core.plugin.{Link, ReceiveHook}
import io.github.gitbucket.solidbase.migration.LiquibaseMigration
import io.github.gitbucket.solidbase.model.Version
import io.github.kounoike.heatmap.controller.HeatMapController
import io.github.kounoike.heatmap.hook.CommitHook

class Plugin extends gitbucket.core.plugin.Plugin {
  override val pluginId: String = "heatmap"
  override val pluginName: String = "Heatmap Plugin"
  override val description : String = "This plugin provides contribution graph as heatmap!"
  override val versions: List[Version] = List(
    new Version("0.0.1", new LiquibaseMigration("update/gitbucket-heatmap_0.0.1.xml"))
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

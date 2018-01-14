package io.github.kounoike.heatmap.model

trait HeatMapCommitComponent { self: gitbucket.core.model.Profile =>
  import profile.api._
  import self._

  lazy val HeatMapCommits  = TableQuery[HeatMapCommits]

  class HeatMapCommits(tag: Tag) extends Table[HeatMapCommit](tag, "HM_COMMIT") {
    val userName = column[String]("USER_NAME", O PrimaryKey)
    val repositoryName = column[String]("REPOSITORY_NAME")
    val branchName = column[String]("BRANCH_NAME")
    val commitId = column[String]("COMMIT_ID")
    val committerMail = column[String]("COMMITTER_MAIL")
    val commitTime = column[java.util.Date]("COMMIT_TIME")

    def * = (userName, repositoryName, branchName, commitId, committerMail, commitTime) <> (HeatMapCommit.tupled, HeatMapCommit.unapply)
  }
}

case class HeatMapCommit(
  userName: String,
  repositoryName: String,
  branchName: String,
  commitId: String,
  committerMail: String,
  commitTime: java.util.Date
)
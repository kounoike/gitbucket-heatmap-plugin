package io.github.kounoike.heatmap.controller

import gitbucket.core.controller.ControllerBase
import gitbucket.core.service.AccountService
import gitbucket.core.util.Implicits._
import gitbucket.core.model.Profile.profile.blockingApi._
import gitbucket.core.model.Profile._
import io.github.kounoike.heatmap.service.HeatMapCommitService

class HeatMapController extends ControllerBase with AccountService with HeatMapCommitService{

  private val commitScore: Int = 1
  private val issueScore: Int = 1
  private val pullScore: Int = 1
  private val commentScore: Int = 1

  get("/:userName/_contribution") {
    val userName = params("userName")
    getAccountByUserName(userName).map { account =>
      gitbucket.heatmap.html.heatmap(account,
        if(account.isGroupAccount) Nil else getGroupsByUserName(userName))
    } getOrElse NotFound()
  }

  ajaxGet("/:userName/_contribution/csv") {
    val userName = params("userName")
    getAccountByUserName(userName).map { account =>
      val commits = getHeatMapCommitsByMailAddress(account.mailAddress)
      val issues = Issues filter { t => t.openedUserName === userName.bind && t.pullRequest === false.bind } list
      val pulls = Issues filter { t => t.openedUserName === userName.bind && t.pullRequest === true.bind } list
      val comments = IssueComments filter { t => t.commentedUserName === userName.bind } list

      "Date,Value\n" + (
        commits.map{ t =>
          s"""${t.commitTime.toInstant.getEpochSecond},${commitScore}"""
        } ++ issues.map { t =>
          s"""${t.registeredDate.toInstant.getEpochSecond},${issueScore}"""
        } ++ pulls.map { t =>
          s"""${t.registeredDate.toInstant.getEpochSecond},${pullScore}"""
        } ++ comments.map { t =>
          s"""${t.registeredDate.toInstant.getEpochSecond},${commentScore}"""
        }
        ).mkString("\n")
    } getOrElse NotFound()
  }
}

package io.github.kounoike.heatmap.controller

import gitbucket.core.controller.ControllerBase
import gitbucket.core.service.{AccountService, RepositoryService}
import gitbucket.core.util.Implicits._
import gitbucket.core.model.Profile.profile.blockingApi._
import gitbucket.core.util.ReferrerAuthenticator
import io.github.kounoike.heatmap.service.HeatMapCommitService
import io.github.kounoike.heatmap.model.HeatmapProfile._

class HeatMapController extends ControllerBase with HeatMapCommitService
  with AccountService with RepositoryService with ReferrerAuthenticator{

  private val commitScore: Int = 1
  private val issueScore: Int = 1
  private val pullScore: Int = 1
  private val commentScore: Int = 1

  get("/:owner/:repository/_pulse")(referrersOnly{ repository =>
    val count = HeatMapCommits.filter{ t =>
      (t.userName === repository.owner.bind) && (t.repositoryName === repository.name.bind)
    }.length.run
    val commits = HeatMapCommits filter { t =>
      (t.userName === repository.owner.bind) && (t.repositoryName === repository.name.bind)
    } groupBy { _.committerMail } map { case (mail, t) =>
      mail -> t.length
    } sortBy { _._2.desc } list
    val commitsByName = commits.zipWithIndex.map{ case ((mail, count), index) =>
      getAccountByMailAddress(mail, true).map{ t => t.fullName -> count }.getOrElse{ s"unknown_${index}" -> count }
      //getAccountByMailAddress(mail, true).map{ t => t.userName -> count }.getOrElse{ mail -> count }
    }
    gitbucket.heatmap.html.pulse(repository, commitsByName, count)
  })

  ajaxGet("/:owner/:repository/_pulse/commits_json")(referrersOnly { repository =>
    val commits = HeatMapCommits filter { t =>
      (t.userName === repository.owner.bind) && (t.repositoryName === repository.name.bind)
    } groupBy { _.committerMail } map { case (mail, t) =>
      mail -> t.length
    } sortBy { _._2 } list
    val commitsByName = commits.map{ case (mail, count) =>
        getAccountByMailAddress(mail, true).map{ t => Map("name" -> t.userName, "count" -> count) }.getOrElse{ Map("name" -> "unknown", "count" -> count) }
    }
    org.json4s.jackson.Serialization.write(commitsByName)
  })

  get("/:userName/_contribution") {
    val userName = params("userName")
    getAccountByUserName(userName).map { account =>
      gitbucket.heatmap.html.heatmap(account,
        if(account.isGroupAccount) Nil else getGroupsByUserName(userName),
        getAccountExtraMailAddresses(userName))
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

  ajaxGet("/:userName/_contribution/json") {
    val userName = params("userName")
    getAccountByUserName(userName).map { account =>
      val commits = getHeatMapCommitsByMailAddress(account.mailAddress)
      val issues = Issues filter { t => t.openedUserName === userName.bind && t.pullRequest === false.bind } list
      val pulls = Issues filter { t => t.openedUserName === userName.bind && t.pullRequest === true.bind } list
      val comments = IssueComments filter { t => t.commentedUserName === userName.bind } list

      val scores = (commits.map{ t =>
          (t.commitTime.toInstant.getEpochSecond -> commitScore)
        } ++ issues.map { t =>
        (t.registeredDate.toInstant.getEpochSecond -> issueScore)
        } ++ pulls.map { t =>
          (t.registeredDate.toInstant.getEpochSecond -> pullScore)
        } ++ comments.map { t =>
          (t.registeredDate.toInstant.getEpochSecond -> commentScore)
        }).groupBy{t => t._1}.mapValues{v => v.map{t => t._2}.sum}
      org.json4s.jackson.Serialization.write(scores)
    } getOrElse NotFound()
  }
}

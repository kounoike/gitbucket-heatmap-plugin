package io.github.kounoike.heatmap.service

import gitbucket.core.model.Session
import io.github.kounoike.heatmap.model.Profile._
import gitbucket.core.model.Profile.profile.blockingApi._
import io.github.kounoike.heatmap.model.HeatMapCommit

trait HeatMapCommitService {
  def getHeatMapCommitsByMailAddress(mail: String)(implicit s: Session): List[HeatMapCommit] = {
    HeatMapCommits filter { t =>
      (t.committerMail === mail.bind)
    } list
  }

  def insertHeatMapCommit(
    userName: String,
    repositoryName: String,
    branchName: String,
    commitId: String,
    committerMail: String,
    commitTime: java.util.Date
  )(implicit s: Session) : Unit = {
    HeatMapCommits insert HeatMapCommit(
      userName,
      repositoryName,
      branchName,
      commitId,
      committerMail,
      commitTime
    )
  }

  def removeHeatMapCommit(
    userName: String,
    repositoryName: String,
    branchName: String
  )(implicit s: Session): Unit = {
    HeatMapCommits filter {t =>
      t.userName === userName.bind && t.repositoryName === repositoryName.bind && t.branchName === branchName.bind
    } delete
  }
}

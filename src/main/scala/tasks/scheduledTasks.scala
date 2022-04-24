package tasks

import bots.VotingBot
import scala.concurrent.{Future, Await}
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object ScheduledTasks {

  def sendReplies(b: VotingBot): Future[Unit] = {
    Future(
      b.chats.foreach(chat =>
        chat._2.toArray
          .maxBy(_._1)
          ._2
          .getPollOptions()
          .foreach(option => {
            Await.ready(
              b.replyToMessage(
                s"Option submitted by ${option._2._3.get.username.getOrElse("Unknown User!")}",
                chat._1,
                option._2._2
              ),
              Duration.Inf
            )
          })
      )
    )
  }

  def sentCountdown(b: VotingBot, time: Int): Future[Unit] = {
    Future {
      b.chats.foreach(x =>
        Await.ready(
          b.sendMessage(
            s"The poll is open!\n You have ${time}s time to answer!",
            x._1
          ),
          Duration.Inf
        )
      )

      Thread.sleep((time / 2) * 1000)

      b.chats.foreach(x =>
        Await.ready(
          b.sendMessage(
            s"Half of the answering time is gone!\n You have ${time / 2}s left to answer!",
            x._1
          ),
          Duration.Inf
        )
      )

      Thread.sleep((time / 4) * 1000)

      b.chats.foreach(x =>
        Await.ready(
          b.sendMessage(
            s"Only 1/4 of the answering time left!\n You have ${time / 4}s left to answer!",
            x._1
          ),
          Duration.Inf
        )
      )

      Thread.sleep((time / 4) * 1000)

      b.chats.foreach(x =>
        Await.ready(
          b.sendMessage(
            "Time is up!",
            x._1
          ),
          Duration.Inf
        )
      )
    }
  }
}

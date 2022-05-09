package tasks

import bots.VotingBot
import scala.concurrent.{Future, Await}
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import com.bot4s.telegram.models.ChatId
import scala.collection.mutable.Map
import utils.Func

object ScheduledTasks {

  /** Finish and stop polls in the specified chat. Also saves data to DB and
    * initializes new polls
    * @param b
    *   The given Bot object
    * @param chatId
    *   The chat in which the polls you want to close are
    * @param time
    *   The Awnser period time
    * @param counter
    *   Counter object for keeping track of polls
    * @return
    */
  def finishChat(
      b: VotingBot,
      chatId: ChatId,
      time: Int,
      counter: utils.Counter
  ): Future[ChatId] = {
    Future {

      val pollId: Int = b.getChat(chatId).head.getLatestPoll()._1

      Await.result(sendReplies(b, chatId, pollId), Duration.Inf)

      Await.result(b.makePoll(pollId, chatId), Duration.Inf)

      sentCountdown(b, time, chatId)

      b.stopPolls(chatId, pollId, b.getChat(chatId).get.getPoll(pollId).get)
        .get
        .foreach(err => {
          if (err.isDefined) {
            print(err.get)
          }
        })

      println(b.chats)

      // Send out results
      b.sendMessage(
        b.getChat(chatId).get.getPoll(pollId).get.representation(),
        chatId
      )

      // Init new poll
      counter.increment()
      b.newPoll(chatId, counter.getCounter(), Func.getTomorrowDate())

      chatId
    }
  }

  /** Send replies to all messages added into the specified poll in the
    * specified chat
    *
    * @param b
    *   The Bot object
    * @param chatId
    *   The chat in which the poll and messages are located
    * @param pollId
    *   The specific poll that you are handling
    * @return
    */
  def sendReplies(
      b: VotingBot,
      chatId: ChatId,
      pollId: Int
  ): Future[Unit] = {
    Future {
      b.getChat(chatId)
        .get
        .getPoll(pollId)
        .get
        .getPollOptions()
        .foreach(option => {
          Await.result(
            b.replyToMessage(
              s"'${option._1}' submitted by ${option._2._3.get.username
                .getOrElse("Unknown User!")}",
              chatId,
              option._2._2
            ),
            Duration.Inf
          )
        })
    }
  }

  /** Initializes and performs countdown for anser period time
    *
    * @param b
    *   Bot object
    * @param time
    *   Answer period time
    * @param chatId
    *   The chat
    */
  def sentCountdown(
      b: VotingBot,
      time: Int,
      chatId: ChatId
  ): Unit = {
    Await.ready(
      b.sendMessage(
        s"The poll is open!\n You have ${time}s time to answer!",
        chatId
      ),
      Duration.Inf
    )

    Thread.sleep((time / 2) * 1000)
    Thread.sleep((time / 4) * 1000)

    Await.ready(
      b.sendMessage(
        s"Only 1/4 of the answering time left!\n You have ${time / 4}s left to answer!",
        chatId
      ),
      Duration.Inf
    )

    Thread.sleep((time / 4) * 1000)

    Await.ready(
      b.sendMessage(
        "Time is up!",
        chatId
      ),
      Duration.Inf
    )

  }
}

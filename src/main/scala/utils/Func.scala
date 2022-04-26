package utils

import bots.VotingBot
import utils.Counter
import tasks.ScheduledTasks

import scala.collection.mutable.Map
import scala.collection.mutable.Buffer
import scala.concurrent.{Future, Await}
import scala.util.{Failure, Success, Try}
import java.util.TimerTask
import java.text.SimpleDateFormat
import java.util.Calendar
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import com.bot4s.telegram.models.ChatId

object Func {
  /*
    Simple function to get current date in specific format
   */
  def getCurrentDate(): String = {
    val format = new SimpleDateFormat("d-M-y")
    format.format(Calendar.getInstance().getTime())
  }

  /*
    Function submitted to scheduled task
   */
  implicit def function2TimerTask(
      function: (VotingBot, Int, Counter) => Unit,
      bot: VotingBot,
      time: Int,
      counter: Counter
  ): TimerTask = {
    return new TimerTask {
      def run() = function(bot, time, counter)
    }
  }

  /*
    The actual task performed during each scheduled work

    Functionality:
      1. Sending out a poll to each chat with all options added today
      2. Closes the polls after specified period
      3. Sends a summary of the polls to each chat
      4. Creates new polls for each chat
   */
  def timerTask(b: VotingBot, time: Int, counter: Counter): Unit = {

    Await.ready(ScheduledTasks.sendReplies(b), Duration.Inf)

    val polls: Map[ChatId, Boolean] = b.makePolls()

    if (!polls.forall(_._2)) {
      println(
        "An error has occurred in chat: " + polls.filter(!_._2).head._1
      )
    }

    Await.ready(ScheduledTasks.sentCountdown(b, time, polls), Duration.Inf)

    b.stopPolls(polls)
      .get
      .foreach(err => {
        if (err.isDefined) {
          print(err.get)
        }
      })

    println(b.chats)

    // Send out results
    b.chats.foreach(x =>
      if (polls(x._1)) {
        b.sendMessage(
          x._2.toArray.sortBy(-_._1).head._2.representation(),
          x._1
        )
      }
    )

    // Init new poll for each chat
    b.chats.keySet.foreach(id => {
      if (polls(id)) {
        counter.increment()
        b.newPoll(id, counter.getCounter(), counter.getCounter().toString())
      }
    })
  }

}

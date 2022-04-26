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

    val validPolls: List[ChatId] =
      b.findValidPolls().toList.filter(_._2).map(_._1)

    def futureOfList: Future[List[ChatId]] =
      Future.traverse(validPolls)(x =>
        ScheduledTasks.finishChat(b, x, time, counter)
      )

    Await.result(futureOfList, Duration.Inf)
  }

}

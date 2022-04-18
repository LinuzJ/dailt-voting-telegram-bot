package utils

import bots.VotingBot
import utils.Counter

import scala.collection.mutable.Buffer
import scala.concurrent.{Future, Await}
import scala.util.{Failure, Success, Try}
import java.util.TimerTask
import java.text.SimpleDateFormat
import java.util.Calendar
import scala.concurrent._
import ExecutionContext.Implicits.global

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

    b.chats.foreach(chat =>
      chat._2.foreach(poll =>
        poll._2
          .getPollOptions()
          .zipWithIndex
          .foreach(option => {
            b.replyToMessage(
              s"Option ${option._2 + 1}",
              chat._1,
              option._1._2._2
            )
          })
      )
    )

    val success: Boolean = b.makePolls()

    // Make sure everything went right before seniding polls
    if (!success) {
      println("Polls not successful")
    }

    b.chats.foreach(x =>
      b.sendMessage(
        s"The poll is open!\n You have ${time}s time to answer!",
        x._1
      )
    )
    Thread.sleep((time / 2) * 1000)
    b.chats.foreach(x =>
      b.sendMessage(
        s"Half of the answering time is gone!\n You have ${time / 2}s left to answer!",
        x._1
      )
    )
    Thread.sleep((time / 4) * 1000)
    b.chats.foreach(x =>
      b.sendMessage(
        s"Only 1/4 of the answering time left!\n You have ${time / 4}s left to answer!",
        x._1
      )
    )

    Thread.sleep((time / 4) * 1000)
    b.chats.foreach(x =>
      b.sendMessage(
        s"Time is up!",
        x._1
      )
    )

    // Stop the current poll
    val fStops: Future[Buffer[Option[String]]] = b.stopPolls()

    fStops onComplete {
      case Success(list) => {
        for (err <- list) {
          if (err.isDefined) { println(err.get) }
        }
      }
      case Failure(t) => {
        println("An error has occurred: " + t.getMessage);
      }
    }

    println(b.chats)

    // Send out results
    b.chats.foreach(x =>
      b.sendMessage(
        x._2.toArray.sortBy(-_._1).head._2.representation(),
        x._1
      )
    )

    // Init new poll for each chat
    b.chats.keySet.foreach(id => {
      counter.increment()
      b.newPoll(id, counter.getCounter(), counter.getCounter().toString())
    })
  }

}

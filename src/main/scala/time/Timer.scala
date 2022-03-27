package time

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import java.util.Calendar;
import java.util.concurrent.TimeUnit
import bots.VotingBot
import com.bot4s.telegram.models.ChatId
import utils.PollData

// TODO: FIX MAKEPOLL REQUESTS

class Timer extends Runnable {

  private var counter: Int = 1
  private var bot: Option[VotingBot] = None
  private var startTime: Calendar = Calendar.getInstance()
  private var currTime: Calendar = Calendar.getInstance()
  private val periodTimeInMinutes: Int = 1
  private val answerPeriodTimeInSeconds: Int = 10

  // Init bot
  def setBot(newBot: VotingBot): Unit = bot = Some(newBot)

  def getCurrTime(): Calendar = currTime

  def elapsedTime(): Long =
    currTime.getTimeInMillis() - startTime.getTimeInMillis()

  // Helpers
  def getCurrentDate(): String =
    currTime.get(Calendar.DATE) + "-" + currTime.get(Calendar.MONTH)
  def getCurrentMinute(): Int = currTime.get(Calendar.MINUTE)

  def run() {
    // Init first poll
    bot match {
      case a: Some[VotingBot] => {
        // Init poll for each chat
        bot.get.chats.foreach(x => {
          bot.get.newPoll(x._1, counter, this.getCurrentDate())
          counter += 1
        })
      }
      case _ =>
    }

    var periodDone: (Boolean, Long) = (false, 0)

    // Timer loop itself
    while (true) {
      // Update time
      currTime = Calendar.getInstance()
      val currElapsedTime: Long = elapsedTime()

      // Check if a chat has been initialized and no poll has been added to it
      if (bot.get.chats.size != 0) {
        val emptyChats
            : Array[(ChatId, scala.collection.mutable.Map[Int, PollData])] = {
          bot.get.chats.toArray.filter(p => p._2.size == 0)
        }

        // Make poll for all empty chats
        for ((chat, poll) <- emptyChats) {
          println(bot.get.chats)
          bot.get.newPoll(chat, counter, this.getCurrentDate())
          println("Added poll!")
          println(bot.get.chats)
          counter += 1
        }
      }

      // Check if specified time period has elapsed
      if (currElapsedTime > 0) {
        if (
          (currElapsedTime % TimeUnit.MINUTES
            .toMillis(periodTimeInMinutes)) == 0
        ) {
          if (
            !periodDone._1 && (currElapsedTime != periodDone._2) && !bot.get.chats.isEmpty
          ) {
            // Update period
            periodDone = (true, currElapsedTime)

            // Send poll and wait for results
            val success: Boolean = bot match {
              case b: Some[VotingBot] => {
                //Call makePolls function from the bot
                b.get.makePolls()
              }
              case _ => false
            }

            if (success) {
              bot.get.chats.foreach(x =>
                bot.get.sendMessage(
                  s"The poll is open!\n You have ${answerPeriodTimeInSeconds}s time to answer!",
                  x._1
                )
              )
              Thread.sleep((answerPeriodTimeInSeconds / 2) * 1000)
              bot.get.chats.foreach(x =>
                bot.get.sendMessage(
                  s"Half of the answering time is gone!\n You have ${answerPeriodTimeInSeconds / 2}s left to answer!",
                  x._1
                )
              )
              Thread.sleep((answerPeriodTimeInSeconds / 4) * 1000)
              bot.get.chats.foreach(x =>
                bot.get.sendMessage(
                  s"Only 1/4 of the answering time left!\n You have ${answerPeriodTimeInSeconds / 4}s left to answer!",
                  x._1
                )
              )

              Thread.sleep((answerPeriodTimeInSeconds / 4) * 1000)
              bot.get.chats.foreach(x =>
                bot.get.sendMessage(
                  s"Time is up!",
                  x._1
                )
              )
              // Stop the current poll
              bot.get.stopPolls()

              // Init new poll for each chat
              bot.get.chats.keySet.foreach(id => {
                bot.get.newPoll(id, counter, this.getCurrentDate())
                counter += 1
              })
            }
          } else {
            periodDone = (false, periodDone._2)
          }
        }
      }
    }
  }
}

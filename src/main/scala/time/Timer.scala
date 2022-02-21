package time

import java.util.Calendar;
import java.util.concurrent.TimeUnit
import bots.VotingBot
import com.bot4s.telegram.models.ChatId

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

      // Check if specified time period has elapsed
      if (currElapsedTime > 0) {
        if (
          (currElapsedTime % TimeUnit.MINUTES
            .toMillis(periodTimeInMinutes)) == 0
        ) {
          if (!periodDone._1 && (currElapsedTime != periodDone._2)) {
            // Update period
            periodDone = (true, currElapsedTime)

            // Send poll and wait for results
            val success: Boolean = bot match {
              case b: Some[VotingBot] => {
                // Latest pollId for each chatId
                var grouped: Map[ChatId, Int] = b.get.chats
                  .groupBy(_._1)
                  .map(x =>
                    (
                      x._1,
                      x._2.toArray
                        .map(_._2.values)
                        .flatten
                        .maxBy(_.getPollId())
                        .getPollId()
                    )
                  )
                // Make poll for each chat
                grouped.forall(chat => b.get.makePoll(chat._2, chat._1))
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

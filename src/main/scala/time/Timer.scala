package time

import java.util.Calendar;
import java.util.concurrent.TimeUnit
import bots.VotingBot

class Timer extends Runnable {

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
      case a: Some[VotingBot] => bot.get.newPoll(this.getCurrentDate())
      case _                  =>
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
                println("made pol")
                b.get.makePoll()
              }
              case _ => false
            }
            if (success) {
              bot.get.sendMessage(
                s"The poll is open!\n You have ${answerPeriodTimeInSeconds}s time to answer!"
              )
              Thread.sleep((answerPeriodTimeInSeconds / 2) * 1000)
              bot.get.sendMessage(
                s"Half of the answering time is gone!\n You have ${answerPeriodTimeInSeconds / 2}s left to answer!"
              )
              Thread.sleep((answerPeriodTimeInSeconds / 4) * 1000)
              bot.get.sendMessage(
                s"Only 1/4 of the answering time left!\n You have ${answerPeriodTimeInSeconds / 4}s left to answer!"
              )
              Thread.sleep((answerPeriodTimeInSeconds / 4) * 1000)
              bot.get.sendMessage(s"Time is up!")
              // Stop the current poll
              bot.get.stopPoll()

              // Init new poll
              bot.get.newPoll(this.getCurrentDate())
            }
          } else {
            periodDone = (false, periodDone._2)
          }

        }
      }
    }
  }
}

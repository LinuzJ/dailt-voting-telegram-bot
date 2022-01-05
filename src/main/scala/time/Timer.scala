package time

import java.util.Calendar;
import java.util.concurrent.TimeUnit
import bots.TestBot

class Timer extends Runnable {

  private var bot: Option[TestBot] = None
  private var startTime: Calendar = Calendar.getInstance()
  private var currTime: Calendar = Calendar.getInstance()
  private val periodTimeInMinutes: Int = 1
  // Init bot
  def setBot(newBot: TestBot): Unit = bot = Some(newBot)

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
      case a: Option[TestBot] => bot.get.newPoll(this.getCurrentDate())
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
            periodDone = (true, currElapsedTime)
            println("mkpoll")
            bot match {
              case b: Option[TestBot] => b.get.makePoll
              case _                  =>
            }
          } else {
            periodDone = (false, periodDone._2)
          }

        }
      }
    }
  }
}

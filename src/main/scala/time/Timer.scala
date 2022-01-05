package time

import java.util.Calendar;
import java.util.concurrent.TimeUnit
import bots.TestBot

class Timer extends Runnable {

  private var bot: Option[TestBot] = None
  private var startTime: Calendar = Calendar.getInstance()
  private var currTime: Calendar = Calendar.getInstance()

  // Init bot
  def setBot(newBot: TestBot): Unit = bot = Some(newBot)

  def getCurrTime(): Calendar = currTime

  def elapsedTime(): Long = TimeUnit.MILLISECONDS.toSeconds(
    Math.abs(currTime.getTimeInMillis() - startTime.getTimeInMillis())
  )

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

    // Timer loop itself
    while (true) {
      currTime = Calendar.getInstance()

      if (elapsedTime() == 60) {
        println("mkpoll")
      }
    }
  }
}

package time

import java.util.Calendar;
import java.util.concurrent.TimeUnit
import bots.CoreBot

class Timer extends Runnable {

  private var bot: CoreBot = _
  private var startTime: Calendar = Calendar.getInstance()
  private var currTime: Calendar = Calendar.getInstance()

  // Init bot
  def setBot(newBot: CoreBot): Unit = bot = newBot

  def getCurrTime(): Calendar = currTime

  def elapsedTime(): Long = TimeUnit.MILLISECONDS.toSeconds(
    Math.abs(currTime.getTimeInMillis() - startTime.getTimeInMillis())
  )

  // Helpers
  def getCurrentDate(): Int = currTime.get(Calendar.DATE)
  def getCurrentMinute(): Int = currTime.get(Calendar.MINUTE)

  def run() {
    while (true) {
      currTime = Calendar.getInstance()
    }
  }
}

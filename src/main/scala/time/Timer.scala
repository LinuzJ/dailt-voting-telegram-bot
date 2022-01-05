package time

import java.util.Calendar;
import java.util.concurrent.TimeUnit

class Timer extends Runnable {
  private var startTime: Calendar = Calendar.getInstance()
  private var currTime: Calendar = Calendar.getInstance()

  def getCurrTime(): Calendar = currTime
  def elapsedTime(): Long = TimeUnit.MILLISECONDS.toSeconds(
    Math.abs(currTime.getTimeInMillis() - startTime.getTimeInMillis())
  )
  def run() {
    while (true) {
      currTime = Calendar.getInstance()
    }
  }
}

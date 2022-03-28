package utils

import bots.VotingBot
import utils.Counter

import java.util.TimerTask
import java.text.SimpleDateFormat
import java.util.Calendar

object Func {

  def getCurrentDate(): String = {
    val format = new SimpleDateFormat("d-M-y")
    format.format(Calendar.getInstance().getTime())
  }

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

  def timerTask(b: VotingBot, time: Int, counter: Counter): Unit = {
    val success: Boolean = b.makePolls()
    if (success) {
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
      b.stopPolls()

      // Init new poll for each chat
      b.chats.keySet.foreach(id => {
        b.newPoll(id, counter.getCounter(), this.getCurrentDate())
        counter.increment()
      })
    }
  }

}

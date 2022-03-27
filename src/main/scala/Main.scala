import bots.VotingBot
import bots.CoreBot

import java.util.TimerTask
import java.util.Timer
import java.util.Calendar
import java.text.SimpleDateFormat

object Main extends App {

  private val key: Option[String] = sys.env.get("TELEGRAM_TOKEN")
  private var bot: VotingBot = _
  private val periodTimeInMinutes: Int = 1
  private val answerPeriodTimeInSeconds: Int = 10
  private var counter: Int = 1

  implicit def function2TimerTask(f: () => Unit): TimerTask = {
    return new TimerTask {
      def run() = f()
    }
  }

  def getCurrentDate(): String = {
    val format = new SimpleDateFormat("d-M-y")
    format.format(Calendar.getInstance().getTime())
  }

  val timer = new Timer()

  if (key.isDefined) {
    bot = new VotingBot(key.get)

    // Init first poll
    bot.chats.foreach(x => {
      bot.newPoll(x._1, counter, this.getCurrentDate())
      counter += 1
    })

    def timerTask() = {
      val success: Boolean = bot.makePolls()
      if (success) {
        bot.chats.foreach(x =>
          bot.sendMessage(
            s"The poll is open!\n You have ${answerPeriodTimeInSeconds}s time to answer!",
            x._1
          )
        )
        Thread.sleep((answerPeriodTimeInSeconds / 2) * 1000)
        bot.chats.foreach(x =>
          bot.sendMessage(
            s"Half of the answering time is gone!\n You have ${answerPeriodTimeInSeconds / 2}s left to answer!",
            x._1
          )
        )
        Thread.sleep((answerPeriodTimeInSeconds / 4) * 1000)
        bot.chats.foreach(x =>
          bot.sendMessage(
            s"Only 1/4 of the answering time left!\n You have ${answerPeriodTimeInSeconds / 4}s left to answer!",
            x._1
          )
        )

        Thread.sleep((answerPeriodTimeInSeconds / 4) * 1000)
        bot.chats.foreach(x =>
          bot.sendMessage(
            s"Time is up!",
            x._1
          )
        )
        // Stop the current poll
        bot.stopPolls()

        // Init new poll for each chat
        bot.chats.keySet.foreach(id => {
          bot.newPoll(id, counter, this.getCurrentDate())
          counter += 1
        })
      }
    }

    timer.schedule(
      function2TimerTask(timerTask),
      periodTimeInMinutes * 20000,
      periodTimeInMinutes * 20000
    )

  } else {
    Console.err.println("Please provide token in env variable")
    System.exit(0)
  }

  val eol = bot.run()
  println("Press ENTER to shutdown the bot")
  scala.io.StdIn.readLine()
  timer.cancel()
  bot.shutdown()

}

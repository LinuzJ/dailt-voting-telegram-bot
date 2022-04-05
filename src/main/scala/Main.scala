import bots.VotingBot
import bots.CoreBot
import utils.Counter
import utils.Func

import java.util.TimerTask
import java.util.Timer
import java.util.Calendar
import java.text.SimpleDateFormat

object Main extends App {

  val timer = new Timer()

  private val key: Option[String] = sys.env.get("TELEGRAM_TOKEN")

  if (!key.isDefined) {
    Console.err.println("Please provide token in env variable")
    System.exit(0)
  }

  private var bot: VotingBot = _
  private val periodTimeInMinutes: Int = 1
  private val answerPeriodTimeInSeconds: Int = 10
  private val counter: Counter = new Counter

  bot = new VotingBot(key.get)

  // Init first poll
  bot.chats.foreach(x => {
    bot.newPoll(x._1, counter.getCounter(), counter.getCounter().toString())
    counter.increment()
  })

  timer.schedule(
    Func.function2TimerTask(
      Func.timerTask,
      bot,
      answerPeriodTimeInSeconds,
      counter
    ),
    periodTimeInMinutes * 20 * 1000,
    periodTimeInMinutes * 20 * 1000
  )

  val eol = bot.run()
  println("Press ENTER to shutdown the bot")
  scala.io.StdIn.readLine()
  timer.cancel()
  bot.shutdown()

}

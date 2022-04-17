import bots.VotingBot
import bots.CoreBot
import utils.Counter
import utils.Func

import java.util.TimerTask
import java.util.Timer
import java.util.Calendar
import java.text.SimpleDateFormat
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends App {

  val timer = new Timer()

  private val key: Option[String] = sys.env.get("TELEGRAM_TOKEN")

  if (!key.isDefined) {
    Console.err.println("Please provide token in env variable")
    System.exit(0)
  }

  private var bot: VotingBot = new VotingBot(key.get)
  private val periodTimeInMinutes: Int = 5
  private val answerPeriodTimeInSeconds: Int = 60
  private val counter: Counter = new Counter

  // Init first poll
  bot.chats.foreach(x => {
    bot.newPoll(x._1, counter.getCounter(), counter.getCounter().toString())
    counter.increment()
  })

  // Schedule the polling
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

  // Run the bot
  val eol = bot.run()

  println("Up and Running!")

  // Add shutdown hook to excecute when closing thread
  sys.addShutdownHook(() => {
    timer.cancel()
    bot.shutdown()
  })

  Await.result(eol, Duration.Inf)

}

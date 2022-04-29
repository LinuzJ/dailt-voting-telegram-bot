import bots.VotingBot
import bots.CoreBot
import utils.Counter
import utils.Func
import utils.ChatEntity

import java.util.TimerTask
import java.util.Timer
import java.util.Calendar
import java.text.SimpleDateFormat
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import db.DBClient

object Main extends App {

  val timer = new Timer()

  private val key: Option[String] = sys.env.get("TELEGRAM_TOKEN")

  private val dbClient = new DBClient

  if (!key.isDefined) {
    Console.err.println("Please provide token in env variable")
    System.exit(0)
  }

  private var bot: VotingBot = new VotingBot(key.get, dbClient)
  private val periodTimeInMinutes: Int = 1
  private val answerPeriodTimeInSeconds: Int = 120
  private val counter: Counter = new Counter

  // Init first poll
  bot.chats.foreach(x => {
    bot.newPoll(x.getId, counter.getCounter(), counter.getCounter().toString())
    counter.increment()
  })

  val today: Calendar = Calendar.getInstance();
  today.set(Calendar.HOUR_OF_DAY, 11);
  today.set(Calendar.MINUTE, 0);
  today.set(Calendar.SECOND, 0);

  // Schedule the polling
  // timer.schedule(
  //   Func.function2TimerTask(
  //     Func.timerTask,
  //     bot,
  //     answerPeriodTimeInSeconds,
  //     counter
  //   ),
  //   today.getTime(),
  //   TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS)
  // )

  // Schedule the polling
  timer.schedule(
    Func.function2TimerTask(
      Func.timerTask,
      bot,
      answerPeriodTimeInSeconds,
      counter
    ),
    periodTimeInMinutes * 60 * 1000,
    periodTimeInMinutes * 60 * 1000
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

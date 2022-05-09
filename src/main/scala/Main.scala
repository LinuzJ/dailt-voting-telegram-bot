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

  private val key: Option[String] = sys.env.get("TELEGRAM_TOKEN")

  if (!key.isDefined) {
    Console.err.println("Please provide token in env variable")
    System.exit(0)
  }

  // Timer for scheduling
  private val timer = new Timer()

  // DB-client communicating with the database
  private val dbClient = new DBClient

  // Counter for keeping track of polls
  private val counter: Counter = new Counter

  // The bot itself providing functionality
  private var bot: VotingBot = new VotingBot(key.get, dbClient)

  // Timer variables
  private val periodTimeInMinutes: Int = 1
  private val answerPeriodTimeInSeconds: Int = 60 * 3
  private val timeOfDay: Int = 19

  // Init first poll
  bot.chats.foreach(x => {
    bot.newPoll(x.getId, counter.getCounter(), counter.getCounter().toString())
    counter.increment()
  })

  // Set time of day to send polls
  val today: Calendar = Calendar.getInstance();
  today.set(Calendar.HOUR_OF_DAY, timeOfDay - 3);
  today.set(Calendar.MINUTE, 0);
  today.set(Calendar.SECOND, 0);

  // Schedule the polling
  timer.schedule(
    Func.function2TimerTask(
      Func.timerTask,
      bot,
      answerPeriodTimeInSeconds,
      counter
    ),
    today.getTime(),
    TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS)
  )

  // Schedule the polling (SHORT VERSION FOR LOCAL TESTING)
  // timer.schedule(
  //   Func.function2TimerTask(
  //     Func.timerTask,
  //     bot,
  //     answerPeriodTimeInSeconds,
  //     counter
  //   ),
  //   periodTimeInMinutes * 30 * 1000,
  //   periodTimeInMinutes * 30 * 1000
  // )

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

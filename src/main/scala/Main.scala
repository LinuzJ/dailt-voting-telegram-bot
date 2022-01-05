import bots.TestBot
import bots.CoreBot
import time.Timer
object Main extends App {

  private val key: Option[String] = sys.env.get("TELEGRAM_TOKEN")
  private var bot: TestBot = _

  if (key.isDefined) {
    val timer: Timer = new Timer

    bot = new TestBot(key.get, timer)

    // INIT TIMER THREAD
    timer.setBot(bot)
    new Thread(timer).start
  } else {
    Console.err.println("Please provide token in .env variable")
    System.exit(0)
  }

  val eol = bot.run()
  println("Press [ENTER] to shutdown the bot, it may take a few seconds...")
  scala.io.StdIn.readLine()
  bot.shutdown()

}

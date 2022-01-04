import bots.TestBot
import bots.CoreBot

object Main extends App {

  private val key: Option[String] = sys.env.get("TELEGRAM_TOKEN")
  private var bot: CoreBot = _

  if (key.isDefined) {
    bot = new TestBot(key.get)
  } else {
    Console.err.println("Please provide token in .env variable")
    System.exit(0)
  }

  val eol = bot.run()
  println("Press [ENTER] to shutdown the bot, it may take a few seconds...")
  scala.io.StdIn.readLine()
  bot.shutdown()

}

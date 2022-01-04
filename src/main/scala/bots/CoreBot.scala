package bots

import com.bot4s.telegram.api.RequestHandler
import com.bot4s.telegram.future.TelegramBot
import com.bot4s.telegram.clients.ScalajHttpClient

import scala.concurrent.Future

/** Core class for the bot
  *
  * @param token
  *   Bot's token.
  */
abstract class CoreBot(val token: String) extends TelegramBot {

  override val client: RequestHandler[Future] = new ScalajHttpClient(token)
}

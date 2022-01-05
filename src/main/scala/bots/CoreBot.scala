package bots

import com.bot4s.telegram.api.RequestHandler
import com.bot4s.telegram.future.TelegramBot
import com.bot4s.telegram.clients.ScalajHttpClient
import com.bot4s.telegram.future.{Polling, TelegramBot}
import com.bot4s.telegram.api.declarative.{Commands, Callbacks}
import com.bot4s.telegram.api.ChatActions
import scala.concurrent.Future
import com.bot4s.telegram.models.ChatId
import com.bot4s.telegram.methods.{SendMessage, _}
import java.util.concurrent.TimeUnit

/** Core class for the bot
  *
  * @param token
  *   Bot's token.
  */
abstract class CoreBot(val token: String)
    extends TelegramBot
    with Polling
    with Commands[Future]
    with Callbacks[Future]
    with ChatActions[Future] {
  var mostRecentChatId: Option[ChatId] = None
  override val client: RequestHandler[Future] = new ScalajHttpClient(token)

  onCommand("initChat") { implicit msg =>
    // Recognize chatId
    mostRecentChatId = Some(ChatId.fromChat(msg.chat.id))

    request(
      SendMessage(
        ChatId.fromChat(msg.chat.id),
        "All done!",
        parseMode = Some(ParseMode.HTML)
      )
    ).map(_ => ())
  }

  def sendMessage(text: String): Unit = {
    mostRecentChatId match {
      case id: Option[ChatId] => {
        request(
          SendMessage(
            id.get,
            text,
            parseMode = Some(ParseMode.HTML)
          )
        )
      }
      case _ => println("No chat Id..")
    }
  }
}

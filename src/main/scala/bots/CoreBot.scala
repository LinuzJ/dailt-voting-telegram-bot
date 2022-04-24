package bots

import utils.PollData
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
import scala.collection.mutable.Map

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

  override val client: RequestHandler[Future] = new ScalajHttpClient(token)

  // Tracking chatIds
  var mostRecentChatId: Option[ChatId] = None

  // Chats (ChatId, (PollId, Polldata))
  var chats: Map[ChatId, Map[Int, PollData]] = Map[ChatId, Map[Int, PollData]]()

  def sendMessage(text: String, chatId: ChatId): Future[Unit] = {
    Future(chatId match {
      case id: ChatId => {
        request(
          SendMessage(
            id,
            text,
            parseMode = Some(ParseMode.HTML)
          )
        )
      }
      case _ => println("No chat Id..")
    })
  }

  def replyToMessage(
      text: String,
      chatId: ChatId,
      replyToMessageId: Int
  ): Future[Unit] = {
    chatId match {
      case id: ChatId => {
        Future {
          request(
            SendMessage(
              id,
              text,
              None,
              None,
              None,
              None,
              None,
              Some(replyToMessageId)
            )
          )
        }
      }
      case _ => Future { println("No chat Id..") }
    }
  }
}

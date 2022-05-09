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
import utils.ChatEntity
import scala.collection.mutable.ArrayBuffer
import com.bot4s.telegram.models.User

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

  // Admin
  private var admin: Option[User] = None

  def getAdmin(): Option[User] = admin
  def setAdmin(u: User) = admin = Some(u)

  // Tracking chatIds
  var mostRecentChatId: Option[ChatId] = None

  // Chats (ChatId, (PollId, Polldata))
  var chats: ArrayBuffer[ChatEntity] = ArrayBuffer[ChatEntity]()

  /** Gets the ChatEntity with the given ChatID
    *
    * @param chatId
    * @return
    */
  def getChat(chatId: ChatId): Option[ChatEntity] = {
    val filt = chats.filter(_.is(chatId))

    if (filt.isEmpty) return None

    Some(filt.head)
  }

  /** Wrapper for sending messages in specified chat
    *
    * @param text
    *   The text you want to send
    * @param chatId
    *   The chat in which you want to send
    * @return
    *   Future
    */
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

  /** Replies to specified message in specified chat
    *
    * @param text
    *   The text you want to send
    * @param chatId
    *   The chat in in which the message you respond to is located
    * @param replyToMessageId
    *   The messageId of the message you are responding to
    * @return
    *   Future
    */
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

package bots

import bots.CoreBot

import scala.concurrent.Future

import com.bot4s.telegram.future.{Polling, TelegramBot}
import com.bot4s.telegram.api.declarative.{Commands, Callbacks}
import com.bot4s.telegram.api.RequestHandler

import com.bot4s.telegram.api.ChatActions
import com.bot4s.telegram._
import com.bot4s.telegram.methods.{SendMessage, _}
import com.bot4s.telegram.models.{
  InlineKeyboardButton,
  InlineKeyboardMarkup,
  InputFile,
  _
}

class TestBot(token: String)
    extends CoreBot(token)
    with Polling
    with Commands[Future]
    with Callbacks[Future]
    with ChatActions[Future] {

  type Button = InlineKeyboardButton
  type Message = com.bot4s.telegram.models.Message

  private var chatId: ChatId = _
  private var message: Message = _
  private var data: String = _
  private var messageId: Int = _

  def getChatId(msg: Message): Long = msg.chat.id

  def killSwitch() = {
    Console.err.println("Shutting down")
    System.exit(0)
    "Quitting"
  }

  /** Reacts and responds to commands without arguments
    *
    * @param command
    *   The name of the command, e.g. "hello" for the command "/hello"
    * @param action
    *   A method that returns a string to send as a reply
    * @return
    */
  onCommand("hello") { implicit msg =>
    request(
      SendMessage(
        ChatId.fromChat(msg.chat.id),
        "Hello young sir!",
        parseMode = Some(ParseMode.HTML)
      )
    ).map(_ => ())
  }

  onCommand("kill") { implicit msg =>
    request(
      SendMessage(
        ChatId.fromChat(msg.chat.id),
        killSwitch(),
        parseMode = Some(ParseMode.HTML)
      )
    ).map(_ => ())
  }

}

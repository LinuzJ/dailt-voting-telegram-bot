package bots

import bots.CoreBot
import Poll

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
import scala.collection.mutable.Map

class TestBot(token: String)
    extends CoreBot(token)
    with Polling
    with Commands[Future]
    with Callbacks[Future]
    with ChatActions[Future] {

  type Button = InlineKeyboardButton
  type Message = com.bot4s.telegram.models.Message

  private var chatId: ChatId = _
  private var polls: Map[String, Poll] = Map[String, Poll]()

  def killSwitch() = {
    Console.err.println("Shutting down")
    System.exit(0)
    "Quitting"
  }

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

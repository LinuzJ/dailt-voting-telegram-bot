package bots

import bots.CoreBot
import utils.PollData

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
  private var polls: Map[String, PollData] = Map[String, PollData]()

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

  onCommand("addPoll") { implicit msg =>
    {
      withArgs { args =>
        {
          val name: Option[String] = args.headOption
          if (name.isDefined) {
            polls(name.get) = new PollData(name.get)
          }
          request(
            SendMessage(
              ChatId.fromChat(msg.chat.id),
              if (name.isDefined) "Success" else "Failiure..",
              parseMode = Some(ParseMode.HTML)
            )
          ).map(_ => ())
        }
      }

    }
  }

  onCommand("viewPolls") { implicit msg =>
    {
      request(
        SendMessage(
          ChatId.fromChat(msg.chat.id),
          (for (poll <- polls.values) yield {
            poll.representation()
          }).mkString("\n"),
          parseMode = Some(ParseMode.HTML)
        )
      ).map(_ => ())
    }
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

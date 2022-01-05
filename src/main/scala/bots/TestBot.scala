package bots

import bots.CoreBot
import utils.PollData
import time.Timer

import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.collection.mutable.Map
import scala.util.{Failure, Success, Try}
import scala.language.postfixOps

import com.bot4s.telegram.future.{Polling, TelegramBot}
import com.bot4s.telegram.api.declarative.{Commands, Callbacks}
import com.bot4s.telegram.api.RequestHandler

import com.bot4s.telegram.api.ChatActions
import com.bot4s.telegram.methods.{SendMessage, _}
import com.bot4s.telegram.models.{
  InlineKeyboardButton,
  InlineKeyboardMarkup,
  InputFile,
  _
}
import simulacrum.op

class TestBot(token: String, timer: Timer)
    extends CoreBot(token)
    with Polling
    with Commands[Future]
    with Callbacks[Future]
    with ChatActions[Future] {

  // Easier types
  type Button = InlineKeyboardButton
  type Message = com.bot4s.telegram.models.Message
  type FutureRe = scala.concurrent.Future[Unit]

  // important vars
  private var chatId: ChatId = _
  private var mostRecentPollMessageId: Int = _
  private var mostRecentPoll: Poll = _

  // data
  private var polls: Map[String, PollData] = Map[String, PollData]()
  private var results: Map[Poll, Array[(String, Int)]] =
    Map[Poll, Array[(String, Int)]]()

  def addToResult(_poll: Poll, _options: Array[PollOption]): Unit = {
    results(_poll) = _options.map(option => {
      (option.text, option.voterCount)
    })
  }

  def sendPoll(_poll: SendPoll) = {
    val f: scala.concurrent.Future[Message] = request(
      _poll
    )
    val result: Try[Message] = Await.ready(f, Duration.Inf).value.get
    val resultEither = result match {
      case Success(t) => {
        mostRecentPollMessageId = t.messageId
        t.poll match {
          case a: Option[Poll] => mostRecentPoll = a.get
          case _               => println("No poll found in message?")
        }
      }
      case Failure(e) => println("Error " + e)
    }
    f.map(_ => ())
  }

  def stopPollAndUpdateData(stop: StopPoll): FutureRe = {
    val f = request(stop)

    val result: Try[Poll] = Await.ready(f, 5 seconds).value.get
    val resultEither = result match {
      case Success(t) => t
      case Failure(e) => println("Error " + e)
    }
    resultEither match {
      case a: Poll => addToResult(a, a.options)
      case _       => println("Something funny has happened")
    }

    return f.map(_ => ())
  }

  onCommand("hello") { implicit msg =>
    println("Most recent poll: " + mostRecentPoll)
    println("Most recent poll message id: " + mostRecentPollMessageId)
    println("results: " + results)
    println("polls: " + polls)
    request(
      SendMessage(
        ChatId.fromChat(msg.chat.id),
        s" ${timer.elapsedTime()}s has elapsed since you turned on the bot and now is minute ${timer
          .getCurrentMinute()} and day ${timer.getCurrentDate()}",
        parseMode = Some(ParseMode.HTML)
      )
    ).map(_ => ())
  }

  onCommand("addPoll") { implicit msg =>
    {
      withArgs { args =>
        {
          val name: Option[String] = args.headOption
          val rest: Array[String] = args.toArray.tail

          if (name.isDefined) {
            polls(name.get) = new PollData(name.get, mostRecentPoll)
            rest.foreach(option => polls(name.get).addOption(option))
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

  onCommand("data") { implicit msg =>
    {
      request(
        SendMessage(
          ChatId.fromChat(msg.chat.id),
          (for ((poll, options) <- results) yield {
            var re: String = poll.id
            options.foreach(x => re = re + " " + x._1 + ": " + x._2)
            re
          }).mkString("\n"),
          parseMode = Some(ParseMode.HTML)
        )
      ).map(_ => ())
    }
  }

  onCommand("makePoll") { implicit msg =>
    val _name = polls.keys.head
    val _poll = polls(_name)
    val f =
      SendPoll(ChatId(msg.chat.id), _name, _poll.getPollOptions().keys.toArray)
    sendPoll(f)
  }

  onCommand("stop") { implicit msg =>
    val s: StopPoll =
      StopPoll(ChatId(msg.chat.id), Some(mostRecentPollMessageId))
    stopPollAndUpdateData(s)
  }

  onCommand("kill") { implicit msg =>
    request(
      SendMessage(
        ChatId.fromChat(msg.chat.id), {
          println("Shutting down")
          System.exit(0)
          "Quitting"
        },
        parseMode = Some(ParseMode.HTML)
      )
    ).map(_ => ())
  }

}

package bots

import bots.CoreBot
import utils.PollData
import time.Timer
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.collection.mutable.Map
import scala.util.{Failure, Success, Try}
import scala.language.postfixOps
import java.util.concurrent.TimeUnit
import com.bot4s.telegram.api.RequestHandler
import com.bot4s.telegram.methods.{SendMessage, _}
import com.bot4s.telegram.models.{
  InlineKeyboardButton,
  InlineKeyboardMarkup,
  InputFile,
  _
}
import simulacrum.op

class VotingBot(token: String, timerIn: Timer) extends CoreBot(token) {

  // Easier types
  type Button = InlineKeyboardButton
  type Message = com.bot4s.telegram.models.Message
  type FutureRe = scala.concurrent.Future[Unit]

  private var mostRecentPollMessageId: Int = _
  private val timer: Timer = timerIn

  def sendPoll(_poll: SendPoll, chatId: ChatId, pollId: Int): Future[Unit] = {
    val f: scala.concurrent.Future[Message] = request(
      _poll
    )
    println("Going to try and send poll")
    val result: Try[Message] = Await.ready(f, 5 seconds).value.get
    println("Poll sent")
    val resultEither = result match {
      case Success(t) => chats(chatId)(pollId).setPollMsg(t.messageId)
      case Failure(e) => println("Error " + e)
    }
    f.map(_ => ())
  }

  def stopPollAndUpdateData(
      chatId: ChatId,
      pollId: Int,
      stop: StopPoll
  ): FutureRe = {
    val f = request(stop)

    val result: Try[Poll] = Await.ready(f, 5 seconds).value.get
    val resultEither = result match {
      case Success(t) => t
      case Failure(e) => println("Error " + e)
    }
    resultEither match {
      case a: Poll => {
        chats(chatId)(pollId).setResult(a, a.options)
        chats(chatId)(pollId).setFinished()
      }
      case _ => println("Something funny has happened")
    }

    return f.map(_ => ())
  }

  def newPoll(chatId: ChatId, id: Int, date: String): Unit = {
    chats(chatId)(id) = new PollData(id, date, chatId)
  }

  def makePoll(pollId: Int, chatId: ChatId): Future[Unit] = {
    if (chats(chatId).exists(_._1 == pollId)) {

      val _date: String = chats(chatId)(pollId).getPollDate()

      if (chats(chatId)(pollId).getPollOptions.keys.size > 0) {
        val _poll: PollData = chats(chatId)(pollId)
        val f =
          SendPoll(
            chatId,
            ("The poll of the day" + _date),
            _poll.getPollOptions().keys.toArray
          )

        sendPoll(f, chatId, pollId)
      } else {
        request(
          SendMessage(
            chatId,
            "There are no poll options for this poll..",
            parseMode = Some(ParseMode.HTML)
          )
        ).map(_ => ())
      }

    } else {
      request(
        SendMessage(
          chatId,
          "There are no poll for this date..",
          parseMode = Some(ParseMode.HTML)
        )
      ).map(_ => ())

    }
  }

  def stopPolls(): Unit = {
    for ((chatId, poll) <- chats) {
      for ((pollid, polldata) <- poll) {
        val s: StopPoll = StopPoll(chatId, Some(polldata.getPollMsg()))
        stopPollAndUpdateData(chatId, pollid, s)
      }
    }
  }

  def findLatestPoll(chatId: ChatId): Option[Int] = {
    Some(chats(chatId).keys.max)
  }

  onCommand("info") { implicit msg =>
    val thisChatId: ChatId = ChatId.fromChat(msg.chat.id)

    request(
      SendMessage(
        thisChatId,
        s" ${TimeUnit.MILLISECONDS.toSeconds(timer.elapsedTime())}s has elapsed since you turned on the bot and now is minute ${timer
          .getCurrentMinute()} and day ${timer.getCurrentDate()}\n\n These are the availible polls ${chats(thisChatId)
          .map(_._2.getPollDate())
          .mkString(" ")}",
        parseMode = Some(ParseMode.HTML)
      )
    ).map(_ => ())
  }

  onCommand("addOption") { implicit msg =>
    {
      withArgs { args =>
        {
          val option: Option[String] = Some(args.mkString(" "))
          val thisChatId: ChatId = ChatId.fromChat(msg.chat.id)
          var re: String = "Error, please try again!"
          if (option.isDefined) {
            if (chats.keySet.contains(thisChatId)) {
              val latest: Int = this.findLatestPoll(thisChatId).get
              // Add option
              chats(thisChatId)(latest).addOption(option.get)

              re = "Optiod added!"
            } else {
              re = "Init the chat first!"
            }
          }
          request(
            SendMessage(
              thisChatId,
              re,
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
          (for (poll <- chats(ChatId.fromChat(msg.chat.id)).map(_._2)) yield {
            poll.representation()
          }).mkString("\n"),
          parseMode = Some(ParseMode.HTML)
        )
      ).map(_ => ())
    }
  }

  onCommand("data") { implicit msg =>
    {
      val thisChatId: ChatId = ChatId.fromChat(msg.chat.id)
      request(
        SendMessage(
          thisChatId,
          (for (poll <- chats(thisChatId).map(_._2)) yield {
            var re: String = poll.getPollDate()
            poll.getResults().foreach(x => re = re + " " + x._1 + ": " + x._2)
            re
          }).mkString("\n"),
          parseMode = Some(ParseMode.HTML)
        )
      ).map(_ => ())
    }
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

  def test(): String = "test"
}

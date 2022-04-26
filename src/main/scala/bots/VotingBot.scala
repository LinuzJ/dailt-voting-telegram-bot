package bots

import bots.CoreBot
import utils.PollData
import utils.Func

import scala.collection.mutable.Buffer
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
import java.text.SimpleDateFormat
import java.util.Calendar;

/** @param token
  *   Bot's token.
  *
  * Main class for the voting bot. Subclass of CoreBot. Keeps track of polls and
  * can send info to the corresponding chats
  */
class VotingBot(token: String) extends CoreBot(token) {

  // Easier types
  type Button = InlineKeyboardButton
  type Message = com.bot4s.telegram.models.Message
  type FutureRe = scala.concurrent.Future[Unit]

  private var mostRecentPollMessageId: Int = _

  def newPoll(chatId: ChatId, id: Int, date: String): Unit = {
    chats(chatId)(id) = new PollData(id, date, chatId)
  }

  def findValidPolls(): Map[ChatId, Boolean] = {
    for ((chatId, poll) <- chats) yield {
      val mostRecetPoll: (Int, PollData) = poll.maxBy(_._1)
      val isValid: Boolean = mostRecetPoll._2.getPollOptions().size > 1
      (chatId -> isValid)
    }
  }

  def makePoll(pollId: Int, chatId: ChatId): Unit = {

    val _name: String = chats(chatId)(pollId).getPollName()

    if (chats(chatId)(pollId).getPollOptions.keys.size > 1) {
      val _poll: PollData = chats(chatId)(pollId)

      sendPoll(_poll.getPollOptions().keys.toArray, _name, chatId, pollId)

    } else if (chats(chatId)(pollId).getPollOptions.keys.size == 1) {
      request(
        SendMessage(
          chatId,
          "There is only one option for this poll, please add another one",
          parseMode = Some(ParseMode.HTML)
        )
      ).map(_ => ())
    }
  }

  def sendPoll(
      options: Array[String],
      _name: String,
      chatId: ChatId,
      pollId: Int
  ): Unit = {
    val f: Future[Message] = request(
      SendPoll(
        chatId,
        ("The poll: " + _name),
        options
      )
    )
    println("Sending poll")
    val f_res = Await.result(f, Duration.Inf)

    f_res match {
      case m: Message   => chats(chatId)(pollId).setPollMsg(m.messageId)
      case e: Throwable => println("Error " + e)
    }
    println("Poll Sent!")
  }

  def stopPollAndUpdateData(
      chatId: ChatId,
      pollId: Int,
      stop: StopPoll
  ): Future[Option[String]] = {

    // Init error message. Also return value for function
    var errorMessage: Option[String] = None

    val f: Future[Poll] = request(stop)

    val result: Try[Poll] = Await.ready(f, 5 seconds).value.get
    val resultEither = result match {
      case Success(t) => {
        t match {
          case a: Poll => {
            chats(chatId)(pollId).setResult(a, a.options)
            chats(chatId)(pollId).setFinished()
          }
          case _ =>
            errorMessage = Some(
              s"Error while adding data to ${pollId} in chat ${chatId}"
            )
        }
      }
      case Failure(e) => println("Error " + e)
    }

    return f.map(_ => errorMessage)
  }

  def stopPolls(
      chatId: ChatId,
      pollId: Int,
      pollData: PollData
  ): Option[Buffer[Option[String]]] = {
    var errorList: Buffer[Option[String]] = Buffer[Option[String]]()

    val s: StopPoll = StopPoll(chatId, Some(pollData.getPollMsg()))

    val fErr: Option[String] =
      Await.result(
        stopPollAndUpdateData(chatId, pollId, s),
        Duration.Inf
      )

    fErr match {
      case e: Option[String] => errorList += e
      case _ =>
        errorList += Some(
          s"Error while adding data to ${pollId} in chat ${chatId}"
        )
    }
    Some(errorList)
  }

  def findLatestPoll(chatId: ChatId): Option[Int] = {
    Some(chats(chatId).keys.max)
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
              re = chats(thisChatId)(latest).addOption(
                option.get,
                msg.messageId,
                msg.from
              )

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
            var re: String = poll.getPollName()
            poll.getResults().foreach(x => re = re + " " + x._1 + ": " + x._2)
            re
          }).mkString("\n"),
          parseMode = Some(ParseMode.HTML)
        )
      ).map(_ => ())
    }
  }

  /*
      The Command for initalizing a chat (adding it to the collection of tracked chats)
   */
  onCommand("init") { implicit msg =>
    val curChatId: ChatId = ChatId.fromChat(msg.chat.id)

    // Recognize chatId
    mostRecentChatId = Some(curChatId)

    if (!chats.keySet.contains(curChatId)) {
      chats(curChatId) = Map[Int, PollData]()

      this.newPoll(chats.head._1, -1, "init")

      request(
        SendMessage(
          ChatId.fromChat(msg.chat.id),
          "Setup done!",
          parseMode = Some(ParseMode.HTML)
        )
      ).map(_ => ())
    } else {
      request(
        SendMessage(
          ChatId.fromChat(msg.chat.id),
          "Chat already setup!",
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

  onCommand("help") { implicit msg =>
    {
      request(
        SendMessage(
          ChatId.fromChat(msg.chat.id),
          "Here are the availible commands:\n - /help\n - /init\n - /addOption\n - /data\n",
          parseMode = Some(ParseMode.HTML)
        )
      ).map(_ => ())
    }
  }

  def test(): String = "test"
}

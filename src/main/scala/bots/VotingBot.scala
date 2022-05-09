package bots

import bots.CoreBot
import utils.PollData
import utils.Func
import utils.ChatEntity
import db.DBClient

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

import java.text.SimpleDateFormat
import java.util.Calendar;
import scala.collection.mutable.ArrayBuffer
import cats.implicits

/** Main class for the voting bot. Subclass of CoreBot. Keeps track of polls and
  * can send info to the corresponding chats
  * @param token
  *   The Telegram token
  * @param db
  *   Database client
  */
class VotingBot(token: String, db: DBClient) extends CoreBot(token) {

  // Easier types
  type Button = InlineKeyboardButton
  type Message = com.bot4s.telegram.models.Message
  type FutureRe = scala.concurrent.Future[Unit]

  private var mostRecentPollMessageId: Int = _

  /** Creates a new PollData object
    *
    * @param chatId
    * @param pollId
    * @param date
    */
  def newPoll(chatId: ChatId, pollId: Int, date: String): Unit = {
    val data: PollData = new PollData(pollId, date, chatId, db)
    this.getChat(chatId).get.addPoll(pollId, data)
  }

  /** Finds all Polls in the currect instance with valid parameters
    *
    * @return
    */
  def findValidPolls(): Map[ChatId, Boolean] = chats
    .map(c => {
      val mostRecetPoll: (Int, PollData) = c.getLatestPoll()
      val isValid: Boolean = mostRecetPoll._2.getPollOptions().size > 1
      (c.getId() -> isValid)
    })
    .to(collection.mutable.Map)

  /** Makes and sends a ready poll to the specified chat
    *
    * @param pollId
    * @param chatId
    * @return
    */
  def makePoll(pollId: Int, chatId: ChatId): Future[Unit] = Future {

    val _name: String =
      this.getChat(chatId).get.getPoll(pollId).get.getPollName()

    if (
      this.getChat(chatId).get.getPoll(pollId).get.getPollOptions.keys.size > 1
    ) {
      val _poll: PollData = this.getChat(chatId).get.getPoll(pollId).get

      sendPoll(_poll.getPollOptions().keys.toArray, _name, chatId, pollId)

    } else if (
      this.getChat(chatId).get.getPoll(pollId).get.getPollOptions.keys.size == 1
    ) {
      Await.ready(
        request(
          SendMessage(
            chatId,
            "There is only one option for this poll, please add another one",
            parseMode = Some(ParseMode.HTML)
          )
        ).map(_ => ()),
        Duration.Inf
      )
    }
  }

  /** Sends a poll to the specified chat with the options provided
    *
    * @param options
    * @param _name
    * @param chatId
    * @param pollId
    */
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
      case m: Message =>
        this.getChat(chatId).get.getPoll(pollId).get.setPollMsg(m.messageId)
      case e: Throwable => println("Error " + e)
    }
    println("Poll Sent!")
  }

  /** Stops given polls and updates the data in the pollData object
    *
    * @param chatId
    * @param pollId
    * @param stop
    * @return
    */
  def stopPollAndUpdateData(
      chatId: ChatId,
      pollId: Int,
      stop: StopPoll
  ): Future[Option[String]] = {

    // Init error message. Also return value for function
    var errorMessage: Option[String] = None

    val f: Future[Poll] = request(stop)

    val result: Try[Poll] = Await.ready(f, Duration.Inf).value.get
    val resultEither = result match {
      case Success(t) => {
        t match {
          case a: Poll => {
            this.getChat(chatId).get.getPoll(pollId).get.setResult(a, a.options)
            this.getChat(chatId).get.getPoll(pollId).get.setFinished()
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

  /** Stops given poll
    *
    * @param chatId
    * @param pollId
    * @param pollData
    * @return
    *   Potential list of errors
    */
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

  /** Returns a list of availible commands
    *
    * @return
    */
  onCommand("help") { implicit msg =>
    {
      val chatId: ChatId = ChatId.fromChat(msg.chat.id)
      this.sendMessage(
        "Here are the availible commands:\n - /help  Lists the most useful commands\n - /init  Initializes a chat\n - /addOption -- Option: Text -- Adds an option to the current poll\n - /polls  Lists all the polls (past and present) from this chat\n - /results  Lists the results of all polls from this chat\n - /options Lists all of the options in the current poll and their indexes\n - /removeOption -- index: Int -- Removes the specified option from the poll",
        chatId
      )

    }
  }

  /** The command for adding an option to the current poll
    *
    * @return
    */
  onCommand("addOption") { implicit msg =>
    {
      withArgs { args =>
        {
          val option: Option[String] = Some(args.mkString(" "))
          val chatId: ChatId = ChatId.fromChat(msg.chat.id)
          var re: String = "Error, please try again!"

          if (option.isDefined) {
            if (chats.exists(_.is(chatId))) {
              val latest: (Int, PollData) =
                this.getChat(chatId).get.getLatestPoll()
              // Add option
              re = latest._2.addOption(
                option.get,
                msg.messageId,
                msg.from
              )

            } else {
              re = "Init the chat first!"
            }
          }
          this.sendMessage(re, chatId)
        }
      }
    }
  }

  /** Remove specified option from the current poll
    *
    * @return
    */
  onCommand("removeOption") { implicit msg =>
    {
      withArgs { args =>
        {
          val chatId: ChatId = ChatId.fromChat(msg.chat.id)
          var re: String = "Error, please try again!"

          val num: Option[Int] =
            try {
              Some(args.mkString("").toInt)
            } catch {
              case e: Throwable => None
            }

          if (num.isDefined) {

            val latest: (Int, PollData) =
              this.getChat(chatId).get.getLatestPoll()
            // Add option

            re = latest._2.deleteOption(
              num.get
            )
          }
          Future()
        }
      }
    }
  }

  /** Sends the options from the current poll
    *
    * @return
    */
  onCommand("options") { implicit msg =>
    {
      Future {
        try {
          val thisChatId: ChatId = ChatId.fromChat(msg.chat.id)

          val msgToSen: String = "Options in the current poll: \n" +
            (for (
              poll <- this
                .getChat(thisChatId)
                .get
                .getLatestPoll()
                ._2
                .getPollOptions
                .zipWithIndex
            )
              yield {
                s"    ${poll._2}  =>  ${poll._1._1}"
              }).mkString("\n")

          this.sendMessage(msgToSen, thisChatId)
        } catch {
          case e: RuntimeException => println(e)
          case _: Throwable =>
            println("Some other exception in while fetching polls")
        }
      }
    }
  }

  /** Sends a list of all of the polls from the chat
    *
    * @return
    */
  onCommand("polls") { implicit msg =>
    {
      Future {
        try {
          val thisChatId: ChatId = ChatId.fromChat(msg.chat.id)
          val r: ArrayBuffer[(String, String, Boolean)] =
            Await.result(db.getPolls(thisChatId), Duration.Inf)

          val msgToSend: String = (for (s <- r) yield {
            s"Poll id: ${s._1}, name: ${s._2}, finished: ${s._3}"
          }).mkString("\n")

          this.sendMessage(msgToSend, thisChatId)
        } catch {
          case e: RuntimeException => println(e)
          case _: Throwable =>
            println("Some other exception in while fetching polls")
        }
      }
    }
  }

  /** Returns the results from the chat. Fetched from the database
    * @return
    */
  onCommand("results") { implicit msg =>
    {
      Future {
        try {
          val thisChatId: ChatId = ChatId.fromChat(msg.chat.id)
          var ids = chats
            .filter(_.is(thisChatId))
            .head
            .getPolls()
            .toArray
            .sortBy(-_._1)
            .tail
            .map(_._1)

          val r: Map[String, ArrayBuffer[(String, String)]] =
            Await.result(
              db.getResults(
                ids,
                thisChatId
              ),
              Duration.Inf
            )

          val msgToSend: String =
            "Results: \n" + (for ((pollId, data) <- r) yield {
              s"  Poll: ${pollId}\n" + (for (res <- data) yield {
                s"    ${res._1} --> votes: ${res._2}"
              }).mkString("\n")
            }).mkString("\n")

          this.sendMessage(msgToSend, thisChatId)
        } catch {
          case e: RuntimeException => println(e)
          case _: Throwable =>
            println("Some other exception in while fetching polls")
        }
      }
    }
  }

  /** Flushes the DB. aka deketes all rows from all tables
    * @return
    */
  onCommand("flushdb") { implicit msg =>
    {
      val usr: User = msg.from.get
      val chatId: ChatId = msg.chat.chatId

      if (!this.getAdmin().isDefined) {
        this.sendMessage("Init admin first", chatId)
      } else {
        if (this.getAdmin().get == usr) {
          Await.ready(db.flushDB(), Duration.Inf)

          this.sendMessage("Database flushed", chatId)

        } else {
          this.sendMessage("You are not the admin", chatId)
        }
      }
    }
  }

  /** The Command for initalizing a chat (adding it to the collection of tracked
    * chats)
    */
  onCommand("init") { implicit msg =>
    val curChatId: ChatId = ChatId.fromChat(msg.chat.id)

    // Recognize chatId
    mostRecentChatId = Some(curChatId)

    if (!chats.exists(_.is(curChatId))) {

      chats += new ChatEntity(curChatId)

      this.newPoll(curChatId, -1, Func.getCurrentDate())

      this.sendMessage("Setup done!", ChatId.fromChat(msg.chat.id))
    } else {
      this.sendMessage("Chat already setup!", ChatId.fromChat(msg.chat.id))
    }
  }

  onCommand("initAdmin") { implicit msg =>
    val cand: User = msg.from.get
    val chatId: ChatId = msg.chat.chatId

    if (this.getAdmin().isDefined) {
      this.sendMessage("Admin already setup..", chatId)
    } else {
      // set admin
      this.setAdmin(cand)

      this.sendMessage("Admin setup!", chatId)
    }
  }

  onCommand("kill") { implicit msg =>
    val usr: User = msg.from.get
    val chatId: ChatId = msg.chat.chatId

    if (this.getAdmin().isDefined) {
      if (this.getAdmin().get == usr) {
        println("Shutting down")
        System.exit(0)
      }
      Future()
    } else {
      this.sendMessage("You are not the admin..", chatId)
    }
  }

  onCommand("capy") { implicit msg =>
    {
      val thisChatId: ChatId = ChatId.fromChat(msg.chat.id)

      this.sendMessage("I'm very capy to be here!", thisChatId)
    }
  }

  def test(): String = "test"
}

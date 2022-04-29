package utils

import scala.collection.mutable.Map
import com.bot4s.telegram.models.Poll
import com.bot4s.telegram.models.ChatId
import com.bot4s.telegram.models.PollOption
import simulacrum.op
import com.bot4s.telegram.models.User

/** Class for a Poll. Keeping track of votes and data.
  *
  * @param id
  *   PollId. Used to track Polls.
  * @param name
  *   The name of this poll.
  * @param chatId
  *   The chatId in which this Poll is initiated
  */
class PollData(val id: Int, name: String, val chatId: ChatId) {

  private val pollName: String = name
  private var options: Map[String, (Int, Int, Option[User])] =
    Map[String, (Int, Int, Option[User])]() // (Text -> (votes, msgId, usr))
  private var pollMsgId: Option[Int] = None
  private var results: Map[String, Int] = Map[String, Int]()

  var isFinished: Boolean = false

  def getPollOptions(): Map[String, (Int, Int, Option[User])] = options
  def getPollName(): String = name
  def getPollId(): Int = id
  def getPollMsg(): Int = pollMsgId.getOrElse(0)
  def getResults(): Map[String, Int] = results

  def setPollMsg(id: Int): Unit = pollMsgId = Some(id)

  def setResult(_poll: Poll, _options: Array[PollOption]): Unit = {
    for (option <- _options) {
      val t: String = option.text
      if (results.keys.toArray.contains(t)) {
        results(t) = results(t) + option.voterCount
      } else {
        results(t) = option.voterCount
      }
    }
  }
  def setFinished(): Unit = isFinished = true

  def addOption(option: String, msgId: Int, usr: Option[User]): String = {
    if (option.length > 40) {
      return "The name is too long, please try again with a shorter name!"
    } else if (option.length < 1) {
      return "The name is too short, please try again with a longer name!"
    }
    options = options + (option -> (0, msgId, usr))

    return "Option added!"
  }

  def deleteOption(name: String): Option[String] = {
    if (!options.keys.exists(_ == name)) {
      return Some("This option does not exist.")
    }

    options.-(name)

    return None
  }

  def representation(): String = {
    var res: String = pollName + ":\n"
    results.foreach(option => {
      res = res + s"  ${option._1} -> ${option._2}\n"
    })
    res
  }

}

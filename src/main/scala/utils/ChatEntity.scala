package utils

import com.bot4s.telegram.models.ChatId
import com.bot4s.telegram.models.User
import scala.collection.mutable.Map

class ChatEntity(private val chatId: ChatId) {

  private val polls: Map[Int, PollData] = Map[Int, PollData]()
  private val admin: Option[User] = None

  /** Add new Polldata If there is 10 polls in the memory already, delete the
    * oldest one
    *
    * @param pollId
    * @param data
    */
  def addPoll(pollId: Int, data: PollData): Unit = {
    if (polls.size == 10) {
      polls.remove(polls.minBy(_._1)._1)
      polls(pollId) = data
    } else {
      polls(pollId) = data
    }
  }

  def getPoll(pollId: Int): Option[PollData] = Some(polls(pollId))
  def getPolls(): Map[Int, PollData] = polls
  def getLatestPoll(): (Int, PollData) = polls.maxBy(_._1)
  def getPreviousPoll(): (Int, PollData) = polls.toArray.sortBy(-_._1).tail.head

  def getId(): ChatId = chatId

  def is(id: ChatId) = id == chatId
}

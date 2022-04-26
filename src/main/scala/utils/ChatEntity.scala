package utils

import com.bot4s.telegram.models.ChatId
import com.bot4s.telegram.models.User
import scala.collection.mutable.Map

class ChatEntity(private val chatId: ChatId) {

  private val polls: Map[Int, PollData] = Map[Int, PollData]()
  private val admin: Option[User] = None

  def addPoll(pollId: Int, data: PollData): Unit = polls(pollId) = data
  def getPoll(pollId: Int): Option[PollData] = {
    if (polls.contains(pollId)) Some(polls(pollId))
    else None
  }
  def getPolls(): Map[Int, PollData] = polls
  def getLatestPoll(): (Int, PollData) = polls.maxBy(_._1)

  def getId(): ChatId = chatId

  def is(id: ChatId) = id == chatId
}

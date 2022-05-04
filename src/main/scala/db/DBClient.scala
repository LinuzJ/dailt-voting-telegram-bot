package db

import java.sql.{Connection, DriverManager, ResultSet}
import scala.collection.mutable.ArrayBuffer
import com.bot4s.telegram.models.User
import scala.concurrent.{Future, Await}
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.collection.mutable.Map
import com.bot4s.telegram.models.ChatId
import cats.instances.future

class DBClient {

//   private val DB_NAME: Option[String] = sys.env.get("POSTGRES_DB")
//   private val DB_USER: Option[String] = sys.env.get("POSTGRES_USER")
  private val DB_NAME: Option[String] = Some("polls")
  private val DB_USER: Option[String] = Some("docker")

  classOf[org.postgresql.Driver]

  val con_str =
    s"jdbc:postgresql://127.0.0.1:5432/${DB_NAME.get}?user=${DB_USER.get}"

  val conn = DriverManager.getConnection(con_str)

  def addPoll(id: Int, name: String, chatId: ChatId): Future[Unit] = {
    Future {
      val stm = conn.createStatement(
        ResultSet.TYPE_FORWARD_ONLY,
        ResultSet.CONCUR_READ_ONLY
      )
      try {
        stm.executeQuery(
          s"""INSERT INTO 
                polls 
                (pollId, name, chatId)
              VALUES 
                (${id}, '${name}', '${chatId.toString()}')
            """
        )
      } catch {
        case e: Throwable => println("ERROR: " + e)
      }

    }
  }

  def addResult(
      id: Int,
      text: String,
      msgId: Option[Int],
      chatId: ChatId,
      votes: Int
  ): Future[Unit] = {
    Future {
      val stm = conn.createStatement(
        ResultSet.TYPE_FORWARD_ONLY,
        ResultSet.CONCUR_READ_ONLY
      )
      try {
        stm.executeQuery(
          s"""INSERT INTO 
                poll_results 
                (chatId, pollId, option_text, msgId, votes) 
              VALUES 
                ('${chatId.toString()}', 
                  ${id},
                  '${text}',
                  ${msgId.getOrElse(-2)},
                  ${votes})
            """
        )
      } catch {
        case e: Throwable => println("ERROR: " + e)
      }

    }
  }

  def getPolls(
      chatId: ChatId
  ): Future[ArrayBuffer[(String, String, Boolean)]] = {
    Future {
      val stm = conn.createStatement(
        ResultSet.TYPE_FORWARD_ONLY,
        ResultSet.CONCUR_READ_ONLY
      )
      var r: ArrayBuffer[(String, String, Boolean)] =
        ArrayBuffer[(String, String, Boolean)]()

      val sql = s"SELECT * from polls WHERE chatId='${chatId.toString()}'"
      val rs = stm.executeQuery(sql)

      while (rs.next) {
        r += (
          (
            rs.getString("pollId"),
            rs.getString("name"),
            rs.getBoolean("finished")
          )
        )
      }
      r
    }
  }

  def getResults(
      ids: Array[Int],
      chatId: ChatId
  ): Future[Map[String, ArrayBuffer[(String, String)]]] = {
    Future {
      val stm = conn.createStatement(
        ResultSet.TYPE_FORWARD_ONLY,
        ResultSet.CONCUR_READ_ONLY
      )
      var res: Map[String, ArrayBuffer[(String, String)]] =
        Map[String, ArrayBuffer[(String, String)]]()

      for (id <- ids) {
        try {
          var r: ArrayBuffer[(String, String)] = ArrayBuffer[(String, String)]()

          val sql =
            s"""SELECT 
                polls.name name,
                poll_results.option_text option_text,
                poll_results.votes votes
              FROM
                poll_results 
                  INNER JOIN 
                polls 
                  ON 
                poll_results.pollId = polls.pollId 
              WHERE 
                poll_results.pollId=${id}
                  AND 
                poll_results.chatId='${chatId.toString()}'
            """
          val rs = stm.executeQuery(sql)

          var name: Option[String] = None
          while (rs.next) {
            if (!name.isDefined) name = Some(rs.getString("name"))
            r += ((rs.getString("option_text"), rs.getString("votes")))
          }
          res(name.get) = r
        } catch { case e: Throwable => println("ERROR:", e) }

      }
      res
    }
  }

  def setFinished(pollId: Int, chatId: ChatId): Future[Unit] = {
    Future {
      val stm = conn.createStatement(
        ResultSet.TYPE_FORWARD_ONLY,
        ResultSet.CONCUR_READ_ONLY
      )
      try {
        stm.executeQuery(
          s"""
          UPDATE
            polls
          SET
            finished=true
          WHERE
            pollId=${pollId}
              AND
            chatId='${chatId.toString()}'
        """
        )
      } catch {
        case e: Throwable => println("ERROR: " + e)
      }
    }
  }
}

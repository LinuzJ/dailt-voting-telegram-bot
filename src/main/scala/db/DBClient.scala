package db

import java.sql.{Connection, DriverManager, ResultSet}
import scala.collection.mutable.ArrayBuffer
import com.bot4s.telegram.models.User
import scala.concurrent.{Future, Await}
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.collection.mutable.Map
import com.bot4s.telegram.models.ChatId

class DBClient {

//   private val DB_NAME: Option[String] = sys.env.get("POSTGRES_DB")
//   private val DB_USER: Option[String] = sys.env.get("POSTGRES_USER")
  private val DB_NAME: Option[String] = Some("polls")
  private val DB_USER: Option[String] = Some("docker")

  classOf[org.postgresql.Driver]

  val con_str =
    s"jdbc:postgresql://localhost:5432/${DB_NAME.get}?user=${DB_USER.get}"

  val conn = DriverManager.getConnection(con_str)

  def addPoll(id: Int, name: String, chatId: ChatId): Future[Unit] = {
    Future {
      val stm = conn.createStatement(
        ResultSet.TYPE_FORWARD_ONLY,
        ResultSet.CONCUR_READ_ONLY
      )

      stm.executeQuery(
        s"INSERT INTO polls (pollId, name, chatId) VALUES (${id}, '${name}', '${chatId.toString()}')"
      )
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

      stm.executeQuery(
        s"INSERT INTO poll_results (chatId, pollId, option_text, msgId, votes) VALUES ('${chatId
          .toString()}', ${id}, '${text}', ${msgId
          .getOrElse(-2)}, ${votes})"
      )
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
  ): Future[Map[Int, ArrayBuffer[(String, String)]]] = {
    Future {
      val stm = conn.createStatement(
        ResultSet.TYPE_FORWARD_ONLY,
        ResultSet.CONCUR_READ_ONLY
      )
      var res: Map[Int, ArrayBuffer[(String, String)]] =
        Map[Int, ArrayBuffer[(String, String)]]()
      for (id <- ids) {
        var r: ArrayBuffer[(String, String)] = ArrayBuffer[(String, String)]()
        val rs =
          stm.executeQuery(
            s"SELECT * from poll_results WHERE pollId=${id} AND chatId='${chatId.toString()}'"
          )
        while (rs.next) {
          r += ((rs.getString("option_text"), rs.getString("votes")))
        }
        res(id) = r
      }
      res
    }
  }
}

package db

import java.sql.{Connection, DriverManager, ResultSet}
import scala.collection.mutable.ArrayBuffer
import com.bot4s.telegram.models.User
import scala.concurrent.{Future, Await}
import scala.concurrent._
import ExecutionContext.Implicits.global

class DBClient {

//   private val DB_NAME: Option[String] = sys.env.get("POSTGRES_DB")
//   private val DB_USER: Option[String] = sys.env.get("POSTGRES_USER")
  private val DB_NAME: Option[String] = Some("polls")
  private val DB_USER: Option[String] = Some("docker")

  classOf[org.postgresql.Driver]

  val con_str =
    s"jdbc:postgresql://localhost:5432/${DB_NAME.get}?user=${DB_USER.get}"

  val conn = DriverManager.getConnection(con_str)

  def addPoll(id: Int, name: String): Future[Unit] = {
    Future {
      val stm = conn.createStatement(
        ResultSet.TYPE_FORWARD_ONLY,
        ResultSet.CONCUR_READ_ONLY
      )

      stm.executeQuery(s"INSERT INTO polls (id, name) VALUES (${id}, ${name})")
    }
  }

  def addResult(
      id: Int,
      text: String,
      msgId: Option[Int],
      votes: Int
  ): Future[Unit] = {
    Future {
      val stm = conn.createStatement(
        ResultSet.TYPE_FORWARD_ONLY,
        ResultSet.CONCUR_READ_ONLY
      )

      stm.executeQuery(
        s"INSERT INTO poll_results (pollId, option_text, msgId, votes) VALUES (${id}, ${text}, ${msgId
          .getOrElse(-2)}, ${votes})"
      )
    }
  }

  def getPolls(): Future[ArrayBuffer[(String, String, Boolean)]] = {
    Future {
      val stm = conn.createStatement(
        ResultSet.TYPE_FORWARD_ONLY,
        ResultSet.CONCUR_READ_ONLY
      )
      var r: ArrayBuffer[(String, String, Boolean)] =
        ArrayBuffer[(String, String, Boolean)]()
      val rs = stm.executeQuery("SELECT * from polls")
      while (rs.next) {
        r += (
          (
            rs.getString("id"),
            rs.getString("name"),
            rs.getBoolean("finished")
          )
        )
      }
      r
    }
  }

  def getResults(id: Int): Future[ArrayBuffer[(String, Int)]] = {
    Future {
      val stm = conn.createStatement(
        ResultSet.TYPE_FORWARD_ONLY,
        ResultSet.CONCUR_READ_ONLY
      )
      var r: ArrayBuffer[(String, Int)] = ArrayBuffer[(String, Int)]()
      val rs =
        stm.executeQuery(s"SELECT * from poll_results WHERE pollId=${id}")
      while (rs.next) {
        r += ((rs.getString("option_text"), rs.getInt("votes")))
      }
      r
    }
  }
}

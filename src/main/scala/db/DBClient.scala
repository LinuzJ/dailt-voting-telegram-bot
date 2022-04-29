package db

import java.sql.{Connection, DriverManager, ResultSet}
import scala.collection.mutable.ArrayBuffer

class DBClient {

//   private val DB_NAME: Option[String] = sys.env.get("POSTGRES_DB")
//   private val DB_USER: Option[String] = sys.env.get("POSTGRES_USER")
  private val DB_NAME: Option[String] = Some("polls")
  private val DB_USER: Option[String] = Some("docker")

  classOf[org.postgresql.Driver]

  val con_str =
    s"jdbc:postgresql://localhost:5432/${DB_NAME.get}?user=${DB_USER.get}"

  def run: ArrayBuffer[String] = {
    val conn = DriverManager.getConnection(con_str)
    var r: ArrayBuffer[String] = ArrayBuffer[String]()
    try {
      val stm = conn.createStatement(
        ResultSet.TYPE_FORWARD_ONLY,
        ResultSet.CONCUR_READ_ONLY
      )

      val rs = stm.executeQuery("SELECT * from polls")

      while (rs.next) {
        r += rs.getString("id")
      }
      r
    } finally {
      conn.close()
    }

  }

}

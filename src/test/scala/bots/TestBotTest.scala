package example

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import bots.VotingBot
import time.Timer

class VotingBotSpec extends AnyFlatSpec with Matchers {
  // Init variables
  val keyTEST: Option[String] = sys.env.get("TELEGRAM_TOKEN")
  val timer: Timer = new Timer
  val x: VotingBot = new VotingBot(keyTEST.get, timer)

  "Test method" should "test" in {
    x.test() shouldEqual "test"
  }
}

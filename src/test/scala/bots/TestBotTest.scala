package example

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import bots.TestBot

class TestBotSpec extends AnyFlatSpec with Matchers {
  "The Hello object" should "say hello" in {
    TestBot.getChatId shouldEqual "hello"
  }
}

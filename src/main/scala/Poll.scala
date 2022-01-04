import scala.collection.mutable.Map

class Poll(name: String) {

  private val pollName: String = name
  private var options: Map[String, Int] =
    Map[String, Int]()

  def getPoll(): Map[String, Int] = options

  def vote(name: String): Option[String] = {
    if (!options.keys.exists(_ == name)) {
      return Some("This option does not exist.")
    }

    options(name) = options(name) + 1

    return None
  }

  def addOption(name: String): Option[String] = {
    if (name.length > 20) {
      return Some("The name is too long, please try again with a shorter name!")
    } else if (name.length < 1) {
      return Some("The name is too short, please try again with a longer name!")
    }

    options = options + (name -> 0)

    return None
  }

  def deleteOption(name: String): Option[String] = {
    if (!options.keys.exists(_ == name)) {
      return Some("This option does not exist.")
    }

    options.-(name)

    return None
  }

}

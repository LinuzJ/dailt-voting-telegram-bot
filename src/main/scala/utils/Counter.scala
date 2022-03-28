package utils

class Counter {
  private var counter: Int = 0

  def increment(): Unit = counter += 1

  def getCounter(): Int = counter
}

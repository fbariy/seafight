package fbariy.seafight
package client

import scala.collection.immutable.Queue

trait FixedQueue[A] {
  def maxSize: Int
  def enqueue(item: A): FixedQueue[A]
  def dequeue: Option[(A, FixedQueue[A])]
  def items: Seq[A]
  def enqueueAll(items: Seq[A]): FixedQueue[A]
}

object FixedQueue {
  def apply[A](size: Int, seq2: A*): FixedQueue[A] =
    impl(size, seq2)

  private def impl[A](size: Int, seq: Seq[A]): FixedQueue[A] =
    new FixedQueue[A] {
      private val queue: Queue[A] = Queue.from[A](seq)

      override def maxSize: Int = size

      override def enqueue(item: A): FixedQueue[A] = {
        val updated =
          if (queue.size + 1 > maxSize) queue.enqueue(item).dequeue._2
          else queue.enqueue(item)

        FixedQueue.impl(maxSize, updated)
      }

      override def dequeue: Option[(A, FixedQueue[A])] =
        queue.dequeueOption.map {
          case (item, updated) => (item, FixedQueue.impl(maxSize, updated))
        }

      override def items: Seq[A] = queue

      override def enqueueAll(items: Seq[A]): FixedQueue[A] =
        items.foldLeft(this: FixedQueue[A])((currentQueue, item) =>
          currentQueue.enqueue(item))
    }
}

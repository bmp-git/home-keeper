package sources

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source

import scala.concurrent.ExecutionContextExecutor
import scala.util.Try


case class RealTimeSourceMulticaster[T](sourceFactory: () => Source[T, Any], errorDefault: T, maxElementBuffered: Int)
                                       (implicit actorSystem: ActorSystem) {
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher
  private type IsLastElement = Boolean
  private type Buffer = scala.collection.mutable.Queue[Option[(IsLastElement, T)]]
  private var bufferContainer = Seq[Buffer]()
  private var working = false
  private val workingLock = new Object()

  private def run(): Unit = {
    workingLock synchronized {
      working = true
    }
    sourceFactory().runForeach(frame => {
      println(bufferContainer.length + " clients with " + bufferContainer.map(_.size).sum + " pending messages")
      var closed = Seq[Buffer]()
      bufferContainer.foreach(b => {
        b synchronized {
          val isLastFrame = b.size > maxElementBuffered
          b.enqueue(Some((isLastFrame, frame)))
          if (isLastFrame) {
            closed = closed :+ b
          }
          b.notify()
        }
      })
      bufferContainer = bufferContainer.filterNot(closed.contains)
    }).onComplete { _ =>
      workingLock synchronized {
        working = false
        bufferContainer.foreach(b => {
          b synchronized {
            b.clear()
            b.enqueue(None)
            b.notify()
          }
        })
        bufferContainer = Seq()
      }
    }
  }

  run()

  private def createNewIterator(): Iterator[T] = new Iterator[T] {
    private val buffer = scala.collection.mutable.Queue[Option[(IsLastElement, T)]]()
    private var continue: Boolean = true
    bufferContainer = bufferContainer :+ buffer

    override def hasNext: Boolean = continue

    override def next(): T =
      buffer.synchronized {
        while (buffer.isEmpty) {
          Try(buffer.wait())
        }
        buffer.dequeue() match {
          case Some((isLastElement, element)) =>
            continue = !isLastElement
            if (isLastElement) {
            }
            element
          case None =>
            continue = false
            errorDefault
        }
      }
  }

  def cast: Source[T, Any] = workingLock synchronized {
    if (working) {
      Source.fromIterator(createNewIterator)
    } else {
      Source.empty[T]
    }
  }

  def isTerminated: Boolean = workingLock synchronized !working

  def retry(): Unit = workingLock synchronized {
    if (!working) {
      run()
    }
  }
}

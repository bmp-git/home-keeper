package sources

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source

import scala.concurrent.ExecutionContextExecutor
import scala.util.Try


case class RealTimeSourceMulticaster[T](sourceFactory: () => Source[T, Any], errorDefault: T, maxElementBuffered: Int,
                                        retryWhenCompleted:Boolean = false)
                                       (implicit actorSystem: ActorSystem) {
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = actorSystem.dispatcher
  private type IsLastElement = Boolean
  private type Buffer = scala.collection.mutable.Queue[Option[(IsLastElement, T)]]
  private var bufferContainer = Seq[Buffer]()
  private var working = false
  private var initializing = false
  private val workingLock = new Object()

  private def run(): Unit = {
    workingLock synchronized {
      initializing = true
    }
    //println("RealTimeSourceMulticaster started")
    sourceFactory().runForeach(frame => {
      workingLock synchronized {
        working = true
      }
      //println(bufferContainer.length + " clients with " + bufferContainer.map(_.size).sum + " pending messages")
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
    }).onComplete { v =>
      //println("RealTimeSourceMulticaster closed " + v)
      workingLock synchronized {
        working = false
        initializing = false
        bufferContainer.foreach(b => {
          b synchronized {
            b.clear()
            b.enqueue(None)
            b.notify()
          }
        })
        bufferContainer = Seq()
      }
      if(retryWhenCompleted) {
        retry()
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
            element
          case None =>
            continue = false
            errorDefault
        }
      }
  }

  def cast: Option[Source[T, Any]] = workingLock synchronized {
    if (working) {
      Some(Source.fromIterator(createNewIterator))
    } else {
      None
    }
  }

  def isTerminated: Boolean = workingLock synchronized !working

  def retry(): Unit = workingLock synchronized {
    if (!initializing) {
      run()
    }
  }
}

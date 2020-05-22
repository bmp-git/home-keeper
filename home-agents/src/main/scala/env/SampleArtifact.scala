package env

import cartago.{Artifact, ArtifactConfig}
import jason.asSyntax.Atom

class SampleArtifact extends Artifact {

  import cartago.OPERATION

  val currentWinner = "no_winner"


  @OPERATION def init(): Unit = { // observable properties
    defineObsProperty("task", "no_task")
    defineObsProperty("winner", new Atom(currentWinner)) // Atom is a Jason type
    defineObsProperty("test", Seq(false.asInstanceOf[AnyRef]) : _*)
  }

  @OPERATION def my_op(): Unit = {
    updateObsProperty("task", "my_task")
    makeArtifact("sam", "env.SampleArtifact2", new ArtifactConfig())
  }

}


class SampleArtifact2 extends Artifact {

  import cartago.OPERATION

  @OPERATION def init(): Unit = { // observable properties
    defineObsProperty("asd", "1")
  }

}

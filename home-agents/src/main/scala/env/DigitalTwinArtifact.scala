package env

import cartago.{Artifact, LINK}
import model.DigitalTwin

abstract class DigitalTwinArtifact extends Artifact {
  def dtInit(dt: DigitalTwin): Unit = {
    dt.properties.foreach(p => {
      defineObsProperty(p.name, p.value, p.semantic)
      println(s"Property ${p.name} created!")
    })
    dt.actions.foreach(a => {
      defineOp(ArtifactOperation(a.name, 0, _ => println(s"Action ${a.name} executed!")), OperationGuard("", 0, _ => true))
      println(s"Action ${a.name} created!")
    })
  }

  @LINK def update(dt: DigitalTwin): Unit = {
    dt.properties.foreach(p => {
      updateObsProperty(p.name, p.value, p.semantic)
      println(s"Property ${p.name} updated!")
    })
  }
}

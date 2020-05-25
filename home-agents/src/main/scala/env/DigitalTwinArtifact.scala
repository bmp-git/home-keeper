package env

import cartago.{Artifact, LINK}
import model.DigitalTwin


abstract class DigitalTwinArtifact extends Artifact {
  def dtInit(dt: DigitalTwin): Unit = {
    dt.properties.foreach(p => {
      //property(name1, value1, semantic1) TODO: BIG PROBLEM
      //property(name2, value2, semantic2)
      defineObsProperty(p.name, p.value, p.semantic)
      println(s"Property ${p.name} created!")
    })
    dt.actions.foreach(a => {
      a.semantic match {
        case "trig" => defineOp(TrigOperation(a.name, s"${dt.url}/actions/${a.name}"), OperationGuard("", 0, _ => true))
        case _ => println(s"Unsupported action semantic ${a.semantic}, action: ${a.name}")
      }
    })
  }

  @LINK def update(dt: DigitalTwin): Unit = {
    dt.properties.foreach(p => {
      updateObsProperty(p.name, p.value, p.semantic)
      println(s"Property ${p.name} updated!")
    })
  }
}

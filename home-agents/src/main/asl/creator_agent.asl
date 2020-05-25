/* Initial beliefs and rules */


/* Initial goals */

!initialize.

/* Plans */

+!initialize : true <-
    makeArtifact("creator", "env.CreatorArtifact", [], H);
    create;
    .send(helloworld, tell, world_created);
    !start.

+!start : true <-
     println("Updating...");
     .wait(1000);
     update;
     !start.
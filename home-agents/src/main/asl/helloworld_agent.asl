/* Initial beliefs and rules */


/* Initial goals */

!start.

/* Plans */

+!start : true <-
     println("Hello, world!");
     .wait({ +world_created[source(creator)] });
     lookupArtifact("firstfloor", F);
     focus(F);
     !work.

+!work : true <-
     ?time(T, S);
     .println(T);
     .wait(1000);
     !work.
!start.

+!start : true <-
     println("Spatio temporal agent started!");
     .wait({ +world_created[source(creator)] });
     lookupArtifact("alarm", AAID);
     focus(AAID).

+events(Events): true <-
    .println("Checking rules...");
    checkSpatialTemporalRule.
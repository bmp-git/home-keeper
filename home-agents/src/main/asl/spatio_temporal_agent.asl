!start.

+!start : true <-
     println("Spatio temporal agent started!");
     .wait({ +world_created[source(creator)] });
     lookupArtifact("home", HAID);
     focus(HAID).

+events(Events): true <-
    .println("Checking rules...");
    checkSpatialTemporalRule.
/* Initial beliefs and rules */


/* Initial goals */

!initialize.

/* Plans */

+!initialize: true <-
    makeArtifact("creator", "env.CreatorArtifact", [], H);
    !create_world.

+!create_world: true <-
    createWorld;
    .println("[INFO] Connected to mirror home service");
    lookupArtifact("users_locator", ULAID);
    focus(ULAID);
    ?users_names(Users);
    for( .member(User, Users)) {
       .create_agent(User, "user_agent.asl");
       .wait(500)
       .send(User, tell, user(User));
    };
    .wait(500);
    .broadcast(tell, world_created);
    !update_world.

-!create_world: true <-
    .println("[ERROR] Error connecting to mirror home service, retrying in 1 second");
    .wait(1000);
    !create_world.

+!update_world: true <-
     .wait(1000);
     updateWorld;
     .println("[INFO] Mirror home status updated correctly");
     !update_world.

-!update_world: true <-
     .println("[ERROR] Error updating mirror home status, retrying in 1 second");
     .wait(1000);
     !update_world.


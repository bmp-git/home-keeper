/* Initial beliefs and rules */


/* Initial goals */

!initialize.

/* Plans */

+!initialize : true <-
    makeArtifact("creator", "env.CreatorArtifact", [], H);
    create;
    lookupArtifact("pages", YPID);
    focus(YPID);
    ?usersNames(Users);
    for( .member(User, Users)) {
       .create_agent(User, "user_agent.asl");
       .wait(500)
       .send(User, tell, user(User));
    };
    .wait(500);
    .broadcast(tell, world_created);
    !start.

+!start : true <-
     println("Updating...");
     .wait(1000);
     updateWorld;
     !start.
/* Initial beliefs and rules */

risk(0).
risk_threshold(1000).

/* Initial goals */

!initialize.

/* Plans */


+!initialize : true <-
     .wait({ +world_created[source(creator)] });
     lookupArtifact("home", HAID);
     focus(HAID);
     lookupArtifact("users_locator", USLO);
     focus(USLO).


+events(Events): true <-
    .print("Events changed: ");
    for( .member(Event, Events)) {
       .print(Event);
       !handle_event(Event);
    };
    .println("").

/* Perimeter door opening */
/* +!handle_event(event(door_open, Name, external, [R1, R2 | _])): true <-*/
+!handle_event(event(door_open, _, external, _)): users_at_home(0) <-
    .println("NO ONE AT HOME ad a door opened!");
    !inc_risk(600).

+!handle_event(Event): true <-
    .println("Handling event: ", Event).

+!inc_risk(Value): true <-
    .println("Incrementing risk");
    ?risk(Old);
    -+risk(Old + Value).

+risk(Value): true <-
    !check_risk.

+!check_risk: true <-
    ?risk(Risk);
    ?risk_threshold(Thr);
    Risk > Thr;
    .println("ALARM! Current risk: ", Risk).

-!check_risk: true <-
    ?risk(Risk);
    .println("Current risk: ", Risk).

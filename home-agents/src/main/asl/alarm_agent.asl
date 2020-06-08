/* Initial beliefs and rules */

risk(100).
risk_threshold(1000).
slot_time_multiplier(1).

/* Initial goals */

!initialize.
!risk_decay.

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

/* No one at home rules. */
/* Perimeter door opening */
+!handle_event(event(door_open, _, external, _)): users_at_home(0) <-
    .println("[NO ONE AT HOME]: an external door opened!");
    !inc_risk(600).

/* Perimeter window opening */
+!handle_event(event(window_open, _, external, _)): users_at_home(0) <-
    .println("[NO ONE AT HOME]: an external window opened!");
    !inc_risk(800).

/* Internal door opening */
+!handle_event(event(door_open, _, internal, _)): users_at_home(0) <-
    .println("[NO ONE AT HOME]: an internal door opened!");
    !inc_risk(100).

/* External motion detection */
+!handle_event(event(motion_detection, room(_, external))): users_at_home(0) <-
    .println("[NO ONE AT HOME]: an external movement is detected!");
    !inc_risk(250).

/* External Gateway motion detection near by */
+!handle_event(event(motion_detection_near, _, external, _)): users_at_home(0) <-
    .println("[NO ONE AT HOME]: an external gateway nearby movement is detected!");
    !inc_risk(150).


/* Someone in a room rules. */
/* Perimeter door opening in a empty room */
/*locations = [user_location(UserName, location(Room))]*/
+!handle_event(event(door_open, _, external, [R1, R2])): users_at_home(Num) & Num > 0 <-
    ?locations(Locations);
    not .member(user_location(_, location(R1)), Locations) & not .member(user_location(_, location(R2)), Locations);
    .println("[SOMEONE IN A ROOM]: an external door connected to an empty room opened!");
    !inc_risk(300).

/* Perimeter door opening in room with a user inside it */
+!handle_event(event(door_open, _, external, [R1, R2])): users_at_home(Num) & Num > 0 <-
    ?locations(Locations);
    .member(user_location(_, location(R1)), Locations) | .member(user_location(_, location(R2)), Locations);
    .println("[SOMEONE IN A ROOM]: an external door connected to a room with an user inside opened!");
    !inc_risk(50).

/* Perimeter window opening in a empty room */
+!handle_event(event(window_open, _, external, [R1, R2])): users_at_home(Num) & Num > 0 <-
    ?locations(Locations);
    not .member(user_location(_, location(R1)), Locations) & not .member(user_location(_, location(R2)), Locations);
    .println("[SOMEONE IN A ROOM]: an external window connected to a empty rooms opened!");
    !inc_risk(500).

/* Perimeter window opening in room with a user inside it */
+!handle_event(event(window_open, _, external, [R1, R2])): users_at_home(Num) & Num > 0 <-
    ?locations(Locations);
    .member(user_location(_, location(R1)), Locations) | .member(user_location(_, location(R2)), Locations);
    .println("[SOMEONE IN A ROOM]: an external window connected to a room with an user inside opened!");
    !inc_risk(100).

/* Internal door opening with a user in a connected room is not a problem */

/* Internal door opening with users not in a connected room is strange */
+!handle_event(event(door_open, _, internal, [R1, R2])): users_at_home(Num) & Num > 0 <-
    ?locations(Locations);
    not .member(user_location(_, location(R1)), Locations) & not .member(user_location(_, location(R2)), Locations);
    .println("[SOMEONE IN A ROOM]: an internal door connected to empty rooms opened!");
    !inc_risk(50).

/* Always on rules. */
/* User get back home */
+!handle_event(event(get_back_home, UserName)): true <-
    .println("[ALWAYS ON]: ", UserName, " get back home!");
    ?risk(Risk);
    !dec_risk(Risk).


+!handle_event(Event): true <-
    .println("Handling event: ", Event).

+!inc_risk(Value): true <-
    .println("Incrementing risk");
    ?risk(Old);
    ?slot_time_multiplier(M);
    -+risk(Old + (Value * M)).

+!dec_risk(Value): true <-
    .println("Decrementing risk");
    ?risk(Old);
    ?slot_time_multiplier(M);
    -+risk(Old - (Value / M)).

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

+!risk_decay: risk(R) & R - 10 >= 0 <-
    !dec_risk(1);
    .wait(1000);
    !risk_decay.

-!risk_decay: true <-
    .wait(1000);
    !risk_decay.


+time_slot(night): slot_time_multiplier(X) & X \== 1.5 <-
    .println("Changing time_slot_multiplier from ", X, " to ", 1.5);
    -+slot_time_multiplier(1.5).

+time_slot(day): slot_time_multiplier(X) & X \== 1 <-
    .println("Changing time_slot_multiplier from ", X, " to ", 1);
    -+slot_time_multiplier(1).
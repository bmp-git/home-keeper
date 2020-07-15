/* Initial beliefs and rules */

risk(0).
risk_threshold(1000).
slot_time_multiplier(1).

/* Rules */
is_internal_room(room(_, RoomName)) :- RoomName \== external.
no_one_at_home :- users_at_home(0).

no_one_in_room(Room) :- locations(Locations) & not .member(user_location(_, location(Room)), Locations).
no_one_in_room(Room1, Room2) :- no_one_in_room(Room1) & no_one_in_room(Room2).

someone_in_room(Room) :- locations(Locations) & .member(user_location(_, location(Room)), Locations).
someone_in_room(Room1, Room2) :- someone_in_room(Room1) | someone_in_room(Room2).

receivers_majority_offline :- receivers_online(On, Tot) & Tot - On >= 2.
receivers_majority_offline :- receivers_online(0, 1).

someone_at_home :- users_at_home(Num) & Num > 0.

everyone_at_home_is_in_a_room :- locations(Locations) & not .member(user_location(_, location(at_home)), Locations).

/* Initial goals */

!initialize.

/* Plans */

+!initialize : true <-
     .wait({ +world_created[source(creator)] });
     lookupArtifact("alarm", AAID);
     focus(AAID);
     lookupArtifact("users_locator", ULAID);
     focus(ULAID);
     lookupArtifact("clock", CAID);
     focus(CAID);
     lookupArtifact("receivers", RAID);
     focus(RAID);
     lookupArtifact("home", HAID);
     focus(HAID);
     +alarmed(false);
     !risk_decay.

+events(Events): true <-
    .print("Events changed: ");
    for( .member(Event, Events)) {
       .print(Event);
       !handle_event(Event);
    };
    .println("").

/* No one at home rules. */
/* A1 - External door opening. */
+!handle_event(event(open, door, _, external, _)): no_one_at_home <-
    .println("[NO ONE AT HOME]: an external door opened!");
    !inc_risk(600).

/* A2 - External window opening. */
+!handle_event(event(open, window, _, external, _)): no_one_at_home <-
    .println("[NO ONE AT HOME]: an external window opened!");
    !inc_risk(800).

/* A3 - Internal door opening. */
+!handle_event(event(open, door, _, internal, _)): no_one_at_home <-
    .println("[NO ONE AT HOME]: an internal door opened!");
    !inc_risk(400).

/* A4 - Internal window opening. */
+!handle_event(event(open, window, _, internal, _)): no_one_at_home <-
    .println("[NO ONE AT HOME]: an internal window opened!");
    !inc_risk(0).

/* A5 - External motion detection. */
+!handle_event(event(motion_detection, room(_, external))): no_one_at_home <-
    .println("[NO ONE AT HOME]: an external movement is detected!");
    !inc_risk(250).

/* A6 - Internal motion detection. */
+!handle_event(event(motion_detection, Room)): is_internal_room(Room) & no_one_at_home <-
    .println("[NO ONE AT HOME]: an internal movement is detected!");
    !inc_risk(150).

/* A7 - External motion detection nearby door. */
+!handle_event(event(motion_detection_near, door, _, external, _)): no_one_at_home <-
    .println("[NO ONE AT HOME]: an external movement nearby door is detected!");
    !inc_risk(150).

/* A8 - External motion detection nearby window. */
+!handle_event(event(motion_detection_near, window, _, external, _)): no_one_at_home <-
    .println("[NO ONE AT HOME]: an external movement nearby window is detected!");
    !inc_risk(150).

/* A9 - A new stranger mac address is detected. */
+!handle_event(event(unknown_wifi_mac, _, _)): no_one_at_home <-
    .println("[NO ONE AT HOME]: a new stranger mac address has been detected!");
    !inc_risk(5).


/* Someone in a room X rules. */
/* B1 - External door opening in a empty room. */
/*locations = [user_location(UserName, location(Room))]*/
+!handle_event(event(open, door, _, external, [R1, R2])): someone_at_home & everyone_at_home_is_in_a_room & no_one_in_room(R1, R2) <-
    .println("[EVERYONE IN A ROOM]: an external door connected to an empty room opened!");
    !inc_risk(600).

/* B2 ✓ - External door opening in room with a user inside it. */
+!handle_event(event(open, door, _, external, [R1, R2])): someone_in_room(R1, R2) <-
    .println("[SOMEONE IN A ROOM]: an external door connected to a room with an user inside opened!");
    !inc_risk(50).

/* B3 - External window opening in a empty room. */
+!handle_event(event(open, window, _, external, [R1, R2])): someone_at_home & everyone_at_home_is_in_a_room & no_one_in_room(R1, R2) <-
    .println("[EVERYONE IN A ROOM]: an external window connected to a empty rooms opened!");
    !inc_risk(800).

/* B4 ✓ - External window opening in room with a user inside it. */
+!handle_event(event(open, window, _, external, [R1, R2])): someone_in_room(R1, R2) <-
    .println("[SOMEONE IN A ROOM]: an external window connected to a room with an user inside opened!");
    !inc_risk(50).

/* B5 ✓ - Internal door opening with a user in a connected room is not a problem. */
+!handle_event(event(open, door, _, internal, [R1, R2])): someone_in_room(R1, R2) <-
    .println("[SOMEONE IN A ROOM]: an internal door connected to a room with someone opened, no problem.");
    !inc_risk(0).

/* B6 - Internal door opening with users not in a connected room is strange. */
+!handle_event(event(open, door, _, internal, [R1, R2])): someone_at_home & everyone_at_home_is_in_a_room & no_one_in_room(R1, R2) <-
    .println("[EVERYONE IN A ROOM]: an internal door connected to empty rooms opened!");
    !inc_risk(50).

/* B7 ✓ - Room X Internal motion detection (not a problem). */
+!handle_event(event(motion_detection, Room)): is_internal_room(Room) & someone_in_room(Room) <-
    .println("[SOMEONE IN A ROOM]: an internal movement is detected in a room with someone!");
    !inc_risk(0).

/* B8 - Room Y Internal motion detection (strange, someone without beacon?). */
+!handle_event(event(motion_detection, Room)): is_internal_room(Room) & someone_at_home & everyone_at_home_is_in_a_room & no_one_in_room(Room) <-
    .println("[EVERYONE IN A ROOM]: an internal movement is detected in a empty room!");
    !inc_risk(25).


/* Someone at home rules. */
/* C1 ✓ - External door opening. */
+!handle_event(event(open, door, _, external, _)): someone_at_home <-
    .println("[SOMEONE AT HOME]: an external door opened!");
    !inc_risk(500).

/* C2 ✓ - External window opening. */
+!handle_event(event(open, window, _, external, _)): someone_at_home <-
    .println("[SOMEONE AT HOME]: an external window opened!");
    !inc_risk(600).

/* C3 ✓ - Internal door opening. */
+!handle_event(event(open, door, _, internal, _)): someone_at_home <-
    .println("[SOMEONE AT HOME]: an internal door opened!");
    !inc_risk(100).

/* C4 ✓ - Internal window opening. */
+!handle_event(event(open, window, _, internal, _)): someone_at_home <-
    .println("[SOMEONE AT HOME]: an internal window opened!");
    !inc_risk(0).

/* C5 ✓ - External motion detection. */
+!handle_event(event(motion_detection, room(_, external))): someone_at_home <-
    .println("[SOMEONE AT HOME]: an external movement is detected!");
    !inc_risk(300).

/* C6 ✓ - Internal motion detection. */
+!handle_event(event(motion_detection, Room)): is_internal_room(Room) & someone_at_home <-
    .println("[SOMEONE AT HOME]: an internal movement is detected!");
    !inc_risk(25).

/* C7 ✓ - External motion detection nearby door. */
+!handle_event(event(motion_detection_near, door, _, external, _)): someone_at_home <-
    .println("[SOMEONE AT HOME]: an external movement nearby door is detected!");
    !inc_risk(150).

/* C8 ✓ - External motion detection nearby window. */
+!handle_event(event(motion_detection_near, window, _, external, _)): someone_at_home <-
    .println("[SOMEONE AT HOME]: an external movement nearby window is detected!");
    !inc_risk(150).

/* C9 ✓ - A new stranger mac address is detected. */
+!handle_event(event(unknown_wifi_mac, _, _)): someone_at_home <-
    .println("[SOMEONE AT HOME]: a new stranger mac address has been detected!");
    !inc_risk(3).


/* Always on rules. */
/* D1 ✓ - User get back home */
+!handle_event(event(get_back_home, UserName)): true <-
    .println("[ALWAYS ON]: ", UserName, " get back home!");
    ?risk(Risk);
    !dec_risk(Risk).

/* D2 ✓ - A receiver went offline. */
+!handle_event(event(receiver_offline, _)): receivers_majority_offline <-
    .println("[ALWAYS ON]: More than one receiver went offline!");
    !inc_risk(1060).

+!handle_event(Event): true <-
    .println("[ERROR] Cannot handle event: ", Event).

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
    .println("Current risk: ", Value);
    !check_risk.

+!check_risk: alarmed(false) <-
    ?risk(Risk);
    ?risk_threshold(Thr);
    Risk >= Thr;
    !turn_alarm_on.

+!check_risk: alarmed(true) <-
    ?risk(Risk);
    ?risk_threshold(Thr);
    Risk < Thr;
    !turn_alarm_off.

+!turn_alarm_on: true <-
    .println("[INFO] Turning alarm on!");
     turnOnAlarm;
     .println("[INFO] SUCCESSFULLY TURNED ALARM ON!");
     -+alarmed(true).

-!turn_alarm_on: true <-
    .println("[ERROR] TURNING ALARM ON!").

+!turn_alarm_off: true <-
    .println("[INFO] Turning alarm off!");
    turnOffAlarm;
    .println("[INFO] SUCCESSFULLY TURNED ALARM OFF!");
    -+alarmed(false).

-!turn_alarm_off: true <-
    .println("[ERROR] TURNING ALARM OFF!").

-!check_risk: true <- true.

+!risk_decay: true <-
    ?risk(R);
    RDec = R * 0.008
    R - RDec >= 0;
    !dec_risk(RDec);
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
/* Initial beliefs and rules */

location(unknown). //location(unknown), location(at_home), location(away), location(room(firstfloor, bedroom))
home_radius(200).  //meters

/* Initial goals */

!start.

/* Plans */

+!start : true <-
     println("Hello, world!");
     .wait({ +world_created[source(creator)] });
     lookupArtifact("receivers", RAID);
     focus(RAID);
     lookupArtifact("clock", CAID);
     focus(CAID);
     lookupArtifact("home", HAID);
     focus(HAID);
     ?user(Name);
     .concat(Name, "_smartphone", Smartphone);
     lookupArtifact(Smartphone, SAID);
     focus(SAID);
     ?location(Place);
     updateUserPosition(Name, Place);
     +status(working);
     !work.

+!work : true <-
    .wait(500);
    ?ble_data(Data);
    ?user(Name);
    .member(userdata(Name, Infos), Data);
    .println(Name, " total infos: ", Infos);
    !check_nearest_receiver_in_space(Infos).

+!check_nearest_receiver_in_space(Infos) : true <-
    ?time(Now);
    userinfo.filters.byTimeDifference(Infos, Filtered, Now, 10000);
    .println("Under 10 seconds infos: ", Filtered);
    userinfo.reducers.maxRssi(Filtered, info(Room, _, _));
    !update_location(Room).

-!check_nearest_receiver_in_space(Infos) : true <-
    .println("No data found in 10 seconds, checking last temporal value in 1 minute");
    !check_nearest_receiver_in_time(Infos).

+!check_nearest_receiver_in_time(Infos) : true <-
    ?time(Now);
    userinfo.filters.byTimeDifference(Infos, Filtered, Now, 60000);
    .println("Under 1 minute infos: ", Filtered);
    userinfo.reducers.maxDate(Filtered, info(Room, _, _));
    !update_location(Room).

-!check_nearest_receiver_in_time(Infos) : true <-
    .println("No data found in 1 minute, checking other systems");
    !check_gps_data.

+!check_gps_data : true <-
    .println("Checking gps data in 10 minutes");
    ?home_location(HomeLat, HomeLon);
    ?time(Time);
    ?smartphone_data(UserLat, UserLon, GpsTime, Accuracy);
    Time - GpsTime < 600000;
    !calculate_gps_distance(HomeLat, HomeLon, UserLat, UserLon, GpsTime, Accuracy).

+!calculate_gps_distance(HomeLat, HomeLon, UserLat, UserLon, GpsTime, Accuracy) :  true <-
    ?home_radius(Radius);
    coordinates.distance(HomeLat, HomeLon, UserLat, UserLon, Distance);
    !check_distance_and_accuracy(Radius, Distance, Accuracy).

+!check_distance_and_accuracy(Radius, Distance, Accuracy): Radius >= Distance + Accuracy <-
    !update_location(at_home).

+!check_distance_and_accuracy(Radius, Distance, Accuracy): Distance >= Radius + Accuracy <-
    !update_location(away).

+!check_distance_and_accuracy(Radius, Distance, Accuracy): Accuracy >= Distance + Radius <-
    !update_location(unknown).

+!check_distance_and_accuracy(Radius, Distance, Accuracy): Distance < Radius <-
    !update_location(at_home).

+!check_distance_and_accuracy(Radius, Distance, Accuracy): Distance >= Radius <-
    !update_location(away).

-!check_distance_and_accuracy(Radius, Distance, Accuracy) : true <-
     .println("[ERROR] Unexpected location: ", Radius, Distance, Accuracy).

-!check_gps_data : true <-
    .println("No valid data found");
    !update_location(unknown).

+!update_location(Room): location(Room) <-
    !work.

+!update_location(room(Floor, Room)): true <-
    ?user(Name);
    updateUserHomePosition(Name, Floor, Room);
    .println("[INFO] Successfully updated position to ", room(Floor, Room));
    -+location(room(Floor, Room));
    !work.

+!update_location(Place): true <-
    ?user(Name);
    updateUserPosition(Name, Place);
    .println("[INFO] Successfully updated position to ", Place);
    -+location(Place);
    !work.

-!update_location(Place): true <-
    .println("[ERROR] Updating position to ", Place).
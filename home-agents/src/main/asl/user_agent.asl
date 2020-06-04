/* Initial beliefs and rules */

location(unknown). //location(unknown), location(at_home), location(away), location(room(firstfloor, bedroom))
home_radius(200).  //meters

/* Initial goals */

!start.

/* Plans */

+!start : true <-
     println("Hello, world!");
     .wait({ +world_created[source(creator)] });
     lookupArtifact("ble_artifact", BAID);
     focus(BAID);
     lookupArtifact("house", HAID);
     focus(HAID);
     ?user(Name);
     .concat(Name, "_smartphone", Smartphone);
     lookupArtifact(Smartphone, SAID);
     focus(SAID);
     +status(working);
     !work.

+!work : true <-
    .wait(500);
    ?receivers(Data);
    ?user(Name);
    .member(userdata(Name, Infos), Data);
    .println(Name, " total infos: ", Infos);
    !check_nearest_receiver_in_space(Infos).

+!check_nearest_receiver_in_space(Infos) : true <-
    userinfo.filters.byTimeDifference(Infos, Filtered, 10000);
    .println("Under 10 seconds infos: ", Filtered);
    userinfo.reducers.maxRssi(Filtered, info(Room, _, _));
    !update_location(Room).

-!check_nearest_receiver_in_space(Infos) : true <-
    .println("No data found in 10 seconds, checking last temporal value in 1 minute");
    !check_nearest_receiver_in_time(Infos).

+!check_nearest_receiver_in_time(Infos) : true <-
    userinfo.filters.byTimeDifference(Infos, Filtered, 60000);
    .println("Under 1 minute infos: ", Filtered);
    userinfo.reducers.maxDate(Filtered, info(Room, _, _));
    !update_location(Room).

-!check_nearest_receiver_in_time(Infos) : true <-
    .println("No data found in 1 minute, checking other systems");
    !check_gps_data;
    !update_location(unknown).

+!check_gps_data : true <-
    .println("Checking gps data in 10 minutes");
    ?home_location(HomeLat, HomeLon);
    ?time(Time);
    ?smartphone(UserLat, UserLon, GpsTime, Accuracy);
    Time - GpsTime < 600000;
    !calculate_gps_distance(HomeLat, HomeLon, UserLat, UserLon, GpsTime, Accuracy).

+!calculate_gps_distance(HomeLat, HomeLon, UserLat, UserLon, GpsTime, Accuracy) :  true <-
    ?home_radius(Radius);
    coordinates.distance(HomeLat, HomeLon, UserLat, UserLon, Distance);
    Radius >= Distance + Accuracy;
    !update_location(at_home).

-!calculate_gps_distance(HomeLat, HomeLon, UserLat, UserLon, GpsTime, Accuracy) : true <-
     !update_location(away).

-!check_gps_data : true <-
    .println("No valid data found");
    !update_location(unknown).

+!update_location(Room) : location(Room) <-
    !work.

-!update_location(Room) : true <-
    -+location(Room).

+location(Place) : status(working) & Place == room(_, _) <-
    .println("I'm in ", Place);
    ?user(Name);
    updateUserHomePosition(Name, Place);
    !work.

+location(Place) : status(working) & Place \== room(_, _) <-
    .println("I'm in ", Place);
    ?user(Name);
    updateUserPosition(Name, Place);
    !work.

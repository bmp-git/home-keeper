/* Initial beliefs and rules */


/* Initial goals */

!start.

/* Plans */

+!start : true <-
     println("Hello, world!");
     .wait({ +world_created[source(creator)] });
     lookupArtifact("ble_artifact", F);
     focus(F);
     !work.

+!work : true <-
    ?receivers(Data);
    .println(Data);
    ?time_now(Now);
    for( .member(userdata(mario, Infos), Data)) {
       for( .member(info(Room, Time, Rssi), Infos)) {
               DT=Now-Time;
               .println(DT)
           }
    }.

/*
+!work : true <-
     ?property(N, T, S);
     .println(T);
     .wait(1000);
     loll;
     !work.*/
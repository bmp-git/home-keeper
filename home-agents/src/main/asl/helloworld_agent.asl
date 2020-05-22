/* Initial beliefs and rules */

home_actions(Actions) :- home(actions(Actions), _, _, _, _).
home_properties(Properties) :- home(_, _, _, properties(Properties), _).
property_value(e(_, _, _, _, value(Value)), Value).

action_name(e(_,name(Name),_,_), Name).

/* Initial goals */

!start.

/* Plans */

+!start : true <-
     println("Hello, world!");
     makeArtifact("creator", "env.CreatorArtifact", [], H);
     create;

     //.findall(Id, lookupArtifactByType("env.FloorArtifact", Id), Ids);

     lookupArtifactByType("env.FloorArtifact", Id);
     focus(Id);
     svg.


/*     makeArtifact("a0", "env.SampleArtifact2", [], H2);
     println(H1);
     println(H2);
     focus(H1);
     ?task(X);
     println(X);
     my_op;
     ?task(Y);
     println(Y);
     lookupArtifact("sam", Id);
     focus(Id);
     ?asd(K);
     println(K).*/
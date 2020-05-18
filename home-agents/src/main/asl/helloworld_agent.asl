/* Initial beliefs and rules */

home_actions(Actions) :- home(actions(Actions), _, _, _, _).
home_properties(Properties) :- home(_, _, _, properties(Properties), _).
property_value(e(_, _, _, _, value(Value)), Value).

action_name(e(_,name(Name),_,_), Name).

/* Initial goals */

!start.

/* Plans */

+!start : true <-
    .wait(home_actions([A|AS]));
    ?action_name(A, Name);
    ?home_properties([P|Ps]);
    ?property_value(P, Value);
	.print(Name);
	.print(Value)
	!start.
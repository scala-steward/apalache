------------------- MODULE sum_types -----------------------
EXTENDS Naturals, FiniteSets
\* VARIABLE x

\* sum type: A type /\ B type => A + B type
Either(A, B) == [left : A, right : B]

\* Intro
InLeft(x) == [left |-> {x}, right |-> {}]
InRight(x) == [left |-> {}, right |-> {x}]

\* Elim

\* @type: ([left: Set(A), right: Set(B)]) => Bool;
IsLeft(e) == Cardinality(e.left) = 1

\* @type: ([left: Set(A), right: Set(B)]) => Bool;
IsRight(e) == Cardinality(e.right) = 1

\* @type: ([left: Set(A), right: Set(B)], A => C, B => C) => C;
EitherElim(e, l(_), r(_)) ==
    IF IsLeft(e) THEN
        l(CHOOSE x \in e.left: TRUE)
    ELSE
        r(CHOOSE x \in e.right: TRUE)


\* @typeAlias: UNIT = Set(Str);
EXTypeAliases == TRUE

UNIT == "__UNIT__"

Option(A) == Either(A, {UNIT})

\* @type: A => [left: Set(A), right: UNIT];
Some(x) == InLeft(x)
\* @type: Set(A) => [left: Set(A), right: UNIT];
None(T) == InRight(UNIT)

\* TODO: Cannot constraint these types to only work on encoding of option...
\* @type: [left: Set(A), right: Set(B)] => Bool;
IsNone(o) == IsRight(o)

\* @type: [left: Set(A), right: Set(B)] => Bool;
IsSome(o) == IsLeft(o)

\* TODO Broken
\* MapSome(T, f(_), o) ==
\*     EitherElim(o, LAMBDA x: Some(f(x)), LAMBDA x: None(T))

\* These(A, B, C) == Either(A, Either(B, C))

\* Some()

------------------------------------------------------------
============================================================
\* Modification History
\* Created Sun Oct  3 21:27:37 2021 by Shon Feder

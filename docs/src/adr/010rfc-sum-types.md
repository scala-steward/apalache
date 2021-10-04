# Sum Types

We would like a way of representing values that inhabit sum types susceptible to
static analysis. The need for this feature is drawn from our own experience, and
from reports received from numerous users trying to write specs.

Users tend to ask for this feature in whatever terms are most familiar to them
from the programming languages they use: we have talked about "enums", "disjoint
unions", "tagged unions", and "variant records". In this RFC, we use the
term "sum types".

## Why sum types matter

Enums (sum types) represent
[[https://en.wikipedia.org/wiki/Disjoint_union][disjoint unions]]. Rather than
trying to wedge something like sum types into TLA+'s record system, what if just
follow the mathematical structure, and use TLA+'s existing support for reasoning
about sets to represent the mathematical structure we need.

We can then use

- If we have a really better way of writing specs, we can forget about backwards compatibility
- Main problem: state variables are used in instance
- Sketch out suggestion.

## Ways we shouldn't encode sum types

### As sets:

```tla
Optional(A) == A

Some(x) == {x}
None(A) == {}

Sub(x, y) == IF x - y < 0 THEN None(Nat) ELSE x - y
```


## Distinct considerations

### How to declare a sum type

### When are two sum types equal


### How to tell when a value is an instance of a sum type

### How to tell when two values are equal instances of a sum type

I.e., what is the canonical form of values inhabiting a sum type.

### Representation in TLA+

## Plan

### Add parametric type aliases

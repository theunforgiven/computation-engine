# computation-engine

### A Scala library for evaluating sequences of computations written as Clojure strings, on domains of arbitrary facts.

### Vision

The goal of this project is to allow arbitrary computations to be specified in a form that
can be manipulated without recompilation. For example, steps in the computation could be stored
in a database and read in at runtime, then changed without having to redeploy the application.
This can be useful, for example, for representing business rules that will be modified frequently.

Arbitrary computations require a generic representation of data and a powerful expression language
for manipulating that data. This library uses Clojure as the expression language; strings representing
Clojure expressions are evaluated in the Clojail sandbox to provide some measure of safety. (However,
this is curently fairly weak; see the "To Dos" below.) Data is represented in generic form as Scala
maps of `Any -> Any` which are converted to untyped Clojure maps.

### How to use it:

1. Instantiate your rules. A Rule has:

    * A namespace - This is used to identify groups of rules that all belong to the same computation.
    * A name - An ordinary-language identifier for the rule.
    * An ordering - Rules in a particular computation are executed in a deterministic sequence, following
    the ordering specified in this field.
    * A transformation expression - A string containing a Clojure expression. This expression should define
    a function that takes an `IPersistentMap` (i.e., a Clojure map) and returns an IPersistentMap. This is
    the expression defining the rule.
    * A flag indicating whether or not the computation should stop if this rule applies. "Applies" means
    that the rule generates a non-empty `IPersistentMap` when applied to a given `IPersistentMap`.
    * A flag indicating whether the rule should throw an exception if it fails to compile or if there is
    an exception when the rule is being applied. Currently this field is not used (see "To Do" below).

2. Instantiate your computation. A computation is instantiated with a list of rules that will be the
steps in the computation.

3. Prepare your data. Your data should be in the form of a Scala immutable Map. This will get converted
to an `IPersistentMap` upon which the Clojure rules will operate. Convenience methods are included in the
`ClojureConversions` object for generating Clojure keywords, lists, and maps.

4. Call your computation's `compute` method. When the `compute`is called with a Scala Map, it converts
the map to an IPersistentMap and runs the rules on that map in sequence until the computation terminates,
either by arriving at the final rule or by applying a rule that sets a termination flag. The application
of each rule results in a new `IPersistentMap` which is the result of combining the existing map with the
new map generated from the Clojure expression. When the computation ends, the final `IPersistentMap` is
converted back to a Scala immutable map.

5. Extract your results. You will need to identify the values in the resulting map that contain the final
results of the application of your rules.

To get a better idea of how this all works, look at the test in the `ComputationTests` class.

### Working with the source code

The gradle build tool is used to build the code. Gradle will also generate project files for IntelliJ IDEA
if the `gradle idea` command is run.

One wrinkle: In IDEA, Scala compiles before Clojure. This causes build failures in
the Scala code that references the Clojail class. In order to build successfully, follow the instructions
in the comments in the `Rule` class for commenting out the Clojail reference to obtain successful Scala and
Clojure builds, and then reenabling the Clojail reference.

### To Do:

1. Implement exception handling when a rule fails to compile or throws an exception
while it is being evaluated. Exception handling should use the switch indicating whether
the exception should be propagated or whether the rule should simply inactivate itself
when an exception is being thrown.

2. The sandbox isn't particularly safe, since it has to allow for defining a Clojure
function. In addition, the function isn't evaluated inside a sandboxed environment.
Additional checks could be implement using regular expressions on the Clojure string
to ensure that it specified a function definition with type hints indicating that it
takes an `IPersistentMap` and returns an `IPersistentMap`.

3. Currently there is no mechanism for testing rules, and when the engine is run in the
debugger it is quite difficult to see how a rule is being evaluated. Ideally there would
be some sort of test harness useful for generating test data and validating rules quickly.

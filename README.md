# computation-engine

### A Scala library for evaluating sequences of computations written as Clojure strings, on domains of arbitrary facts.

### Vision

The goal of this project is to allow arbitrary computations to be specified in a form that
can be manipulated without recompilation. For example, steps in the computation could be stored
in a database and read in at runtime, then changed without having to redeploy the application.
This can be useful, for example, for representing business rules that will be modified frequently.

Arbitrary computations require a generic representation of data and a powerful expression language
for manipulating that data. This version of the computation-engine library uses Clojure as the 
expression language; strings representing Clojure expressions are evaluated in the Clojail sandbox 
to provide some measure of safety. (However, this is curently not well tested; see the "To Dos" below.) 
Data is represented in generic form as Scala maps of `Any -> Any` which are converted to untyped Clojure 
maps of maps.

### How to use it:

1. Instantiate your simple computations. A `SimpleComputation` has:
    * A namespace - This is used to identify groups of computations that all belong to the same sequence
    of computations.
    * A name - An ordinary-language identifier for the computation.
    * An ordering - Computations in a particular sequence are executed in a deterministic order, following
    the ordering specified in this field.
    * A transformation expression - A string containing a Clojure expression. This expression should define
    an expression that contains free bindings representing Clojure maps, and that evaluates to a value (ideally
    also a Clojure map). This is the expression defining the computation.
    * An input map, mapping the free variables in the transformation expression to the keys in the data map
    that will be passed to the computation (see #3 below).
    * An output map, mapping an identifier (which should not be one of the free variables in the transformation
    expression) to the key in the output data map that points to the results of the computation. This map should
    have only one entry.
    * A flag indicating whether or not the computation should stop if this computation applies. "Applies" means
    that the computation (not the expression) generates a nonempty map. (In the current implementation, this
    always happens unless the computation finds no data on which to operate. See the "To Do" section below.)
    * A flag indicating whether the computation should throw an exception if the expression fails to compile or
    if there is an exception when the computation is being applied. Currently this field is not used (see "To Do" below).
2. Instantiate your final computation. A `SimpleComputation` is for a single step; if you want to chain a number
of computations together, use a `SequentialComputation` instantiated with a list of `SimpleComputations`.

3. Prepare your data. Your data should be in the form of a Scala immutable `Map`. This will get converted
to an `IPersistentMap` upon which the Clojure computations will operate. Convenience methods are included in the
`ClojureConversions` object for generating Clojure keywords, lists, and maps.

4. Call your computation's `compute` method. When the `compute` method is called on a `SequentialComputation` with a
Scala `Map`, it converts the map to an IPersistentMap and applies the computation steps on that map in sequence
until the computation terminates, either by arriving at the final computation or by applying a computation that sets a
termination flag. The application of each computation results in a new `IPersistentMap` which is the result of
merging the existing map with the new map generated from the Clojure expression. When the computation ends,
the final `IPersistentMap` is converted back to a Scala immutable map. (If the `compute` method is called on
a `SimpleComputation` only, just the result map is returned and converted back to a Scala immutable map. This
should probably be made parallel to the `SequentialCombination` and combine the results with the map that was
originally passed in; see "To Do" below.)

5. Extract your results. You will need to identify the values in the resulting map that contain the final
results of the application of your rules.

To get a better idea of how this all works, look at the tests in the `ComputationTests` class.

### Under the hood

Your Clojure expression will be surrounded with the following form:

    (fn ^clojure.lang.IPersistentMap [^clojure.lang.IPersistentMap domain-facts]
              (let [$letMappings]
               (if $emptyCheckExpression
                {}
                (let [$outputBinding $transformationExpression]
                  (hash-map $outputDomainKey $outputBinding)))))

where $letMappings is a string taking the keys and values of the input map passed to the constructor of the
computation (the values being keys of the data map passed to the computation). The $letMappings string is
of the form:

    $inputMapKey (domain-facts $inputMapValue)

The $emptyCheckExpression checks to see if any of the let bindings are empty (i.e. the value bindings taken
from the incoming data map), and if so returns an empty map from the computation.
The $emptyCheckExpression is of one of the following two forms:

    (empty? $binding)
    (and (empty? $binding) (empty? $binding) ...)

Note that this assumes that the values are collections, with the result that the incoming data map must be
a map whose values are collections.

The result of evaluating $transformationExpression is bound to $outputBinding and then made the value for
$outputDomainKey in the map that is the result of the computation. The $outputBinding and $outputDomainKey
are the key and value in the output map that is passed to the constructor of the computation. (This is
somewhat redundant; see the "To Do" section below.)

Note that the computation engine requires some data to be passed into the computation. It is envisioned
that these computations be stateless, with their results dependent only on the original values passed in.
If you find yourself writing a computation that takes no inputs, you're probably doing it wrong. E.g., a
rule that adds a day to the current date should take the current date as input rather than asking the system
for the current date.

### Using the jar

Using this library requires a java policy file. To get off the ground, you can create a policy file and
point your application to this policy file by using the Java system property switches on your application's
command line; e.g.:

    -Djava.security.manager -Djava.security.policy==[PATH_TO_FILE]/.computation-engine.policy

A sample (extremely liberal) policy file is included in the src/test/resources directory.

If you publish the computation-engine jar file to a Maven repository and use it from Maven or a similar
build tool, you will need to add the Clojars repository (http://clojars.org/repo) as well as reference
clojure (org.clojure:clojure:1.5.1) and clojail (clojail:clojail:1.0.6) in your build file. Currently the
computation-engine jar file actually contains all the necessary code (it is compiled in by the AOT compile),
but the clojure and clojail dependencies still wind up in the pom file, with the result that pulling in the
computation-engine jar tries to fetch the clojure and clojail jars anyway. (This is another fix under "To
Do" below.)

### Working with the source code

The gradle build tool is used to build the code. Gradle will also generate project files for IntelliJ IDEA
if the `gradle idea` command is run.

Running the tests for this library requires a java policy file. You can use the sample policy file
src/test/resources/sample.policy by specifying it on the gradle command line; e.g.:

    gradle -Djava.security.policy=src/test/resources/sample.policy -Djava.security.manager test --info

One wrinkle: In IDEA, Scala compiles before Clojure. This causes build failures in
the Scala code that references the Clojail class. In order to build successfully, follow the instructions
in the comments in the `SimpleComputation` class for commenting out the Clojail reference to obtain successful
Scala and Clojure builds, and then reenabling the Clojail reference.

### To Do:

This library still has a lot of rough edges and is still more or less at the proof-of-concept stage.
In particular, when implementing it in practice, it proved more cumbersome than expected to convert Scala
maps back and forth to IPersistentMap. (Partly this is because Scala often relies on implicit conversions
to convert Scala data structures to Java ones, rather than on implementing Java collection interfaces.)
In addition, the quantity of validation and error-checking logic required inside rules made them more
inelegant than had been hoped.

Development of this library is moving on to allow representation of rules as Scala strings, and the
Clojure version will be kept on a side branch. For anyone who wants to continue development, however,
here are some things that could use improvement:

1. The output map parameter to the `SimpleComputation` constructor is obsolete. It should be
enough to pass a single value representing a key for obtaining the computation results from
the data map that is the result of the computation.

2. A computation always inserts something into the returned data map even if it does nothing,
which means that the flag for aborting the computation early is not working properly. This
needs to be fixed.

3. The Gradle build should not include the clojure and clojail dependencies in the pom file, since
the necessary classes are AOT compiled into the computation-engine jar file anyway.

4. Exception handling needs to be implemented when a computation fails to compile or throws an exception
while it is being evaluated. Exception handling should use the switch indicating whether
the exception should be propagated or whether the computation should simply inactivate itself
(and write to a log) when an exception is being thrown.

5. The `SimpleComputation` should work the same way as the `CompoundComputation` and combine its
results with the map originally passed in to the computation.

6. Namespace and ordering should not be attributes of computations. They might be fields stored in a
database along with the computation, but are really metadata used to group and order
computations. Since this grouping and ordering can change depending on where a computation
is used, it doesn't belong on the computation (and is in fact never referenced in the computation).

7. Conversions for Scala symbols to Clojure keywords would be useful.

8. Some of the code makes assumptions that the structures being passed to a rule are maps
of maps (e.g., the values are tested by the Clojure wrapper to see if they are empty). This
constraint should probably be relaxed.

9. The security of the sandbox hasn't been tested and probably needs some work.

10. Currently there is no mechanism for testing computations, and when the engine is run in the
debugger it is quite difficult to see how a computation is being evaluated. Ideally there would
be some sort of test harness useful for generating test data and validating rules quickly.

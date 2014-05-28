# computation-engine

### A Scala library for evaluating sequences of computations written as Scala strings, on domains of arbitrary facts.

### Vision

This library allows arbitrary computations to be specified in a form that can be manipulated without
recompiling the application that uses them. Steps in the computation can be stored in a database and
read in at runtime, then changed without having to deploy new code. This can be
useful, for example, for representing business rules that will be modified frequently.

Arbitrary computations require a generic representation of data and an expression language
for manipulating that data. This library uses Scala as the expression language; strings representing
Scala expressions are evaluated in the Scala Script Engine sandbox to provide some safety. Data is
represented as a domain of facts represented generically as Scala maps of `Symbol -> Any`.

The computation engine provides several types of compound computation to allow the re-use of
simpler computations in different contexts. For example, you may want to apply a computation to a
single value in one place in your application, but apply the same computation to a series or map
of values in another (using an `IterativeComputation,` `MappingComputation,` or `FoldingComputation`). If you need to
change the rule represented by the computation, re-using the same computation in both contexts
lets you make changes in just one place. Similarly, chaining computations together in a sequence
(using a `SequentialComputation`) allows the steps in the chained computation to be used separately
elsewhere.

### How to use it:

The core library can be obtained from Maven Central with the group ID `com.cyrusinnovation.computation-engine`
and the artifact ID `computation-engine-core`. To code against it:

1. Instantiate your computations. (For details, see "Creating computations" below.)

2. Prepare your data. The facts in the domain passed to the computation should be in the form of a
Scala immutable `Map[Symbol, Any]`.

3. Call your computation's `compute` method.

4. Extract your results. The output of a computation is a `Map[Symbol, Any]` that includes the original data
passed to the computation, plus a map entry mapping the resultKey to the result of the computation. In a
`SequentialComputation` the output also contains map entries corresponding to the result keys to the
results of the intermediate computations. In an `IterativeComputation` the output does not contain the
results of the individual iterations; the only result is an entry with the computation's result key mapped
to the single result of all the iterations combined (returned as a List).

To get a better idea of how this all works, look at the tests for the various computation classes.

### Creating computations

A `SimpleComputation` has:
* A package name - This will be the package name of the class compiled from the computation.
* A name - This will be the name of the class compiled from the computation, and so ideally should
  follow Java upper-camel-case naming style.
* A description - an ordinary-language description of the computation.
* Imports - A list of fully-qualified classnames or other strings that each could be used in a
  Scala `import` statement (without the "import" keyword).
* The computation expression - A string containing a Scala expression. This expression should contain
  unbound identifiers that will be bound using the input map below. Its result should be of `Option[Any]`
  type.
* An input identifier mapping - A map of `String -> Symbol` whose keys are of the form `identifer:Type`
  where the identifiers are unbound in the computation expression and `Type` designates the type of the val
  that will be created using the identifer. The values of this input identifier mapping are symbols designating
  the keys in the domain of facts that will be bound to the specified vals.
* A result key - A symbol that will be the key for the result of the computation. The output
  of the computation is a map containing the entire input data map as well as an entry for the result,
  using the result key as the key and the computation's result as the value.
* A security configuration - An instance of the `SecurityConfiguration` trait indicating what packages
  are safe to load, what classes in those packages are unsafe to load, and where the Java security policy
  file for the current security manager is.
* A logger - An instance of `com.cyrusinnovation.computation.util.Log`. A convenience case class
  `com.cyrusinnovation.computation.util.ComputationEngineLog` extends this trait and wraps an slf4j
  log passed to its constructor.
* A "should propagate exceptions" flag - This flag indicates whether the computation should rethrow an
  exception when the expression is compiled or applied.

A `SequentialComputation` is instantiated with a list of computations that will be the steps in the
sequence.

An `IterativeComputation` performs the same computation on a sequence of values, resulting in a sequence of
results in the same order. Its constructor takes as its first parameter the computation to be applied. Next,
it takes a tuple of symbols that specifies its input: The first element identifies the key in the domain of
facts whose value is the sequence to be iterated over, and the second element is the key in the inner
computation to which each value from the sequence should be assigned in turn. Finally,
the constructor takes a symbol identifying the result key for the iterative computation; the computation's
result will be a list that is assigned to that result key in the result map. (The results of the inner
computation are extracted using the inner computation's result key.)

A `MappingComputation` also takes a single computation over which it will iterate, but the values to be
iterated over are the values of a map rather than a simple sequence. The result of each application of the
inner computation is assigned to the original key in the map returned in the results. The constructor of a
`MappingComputation` is the same as that of an `IterativeComputation.`

A `FoldingComputation` folds or reduces a computation (leftwards) over a sequence of values; its result is the
accumulated value. It takes, first, a symbol that will point to the initial value of the accumulator in the
input domain. Next, it takes a tuple of symbols whose first element points to the sequence of values in
the input domain, and whose second element is the key in the inner computation to which a single value
should be assigned. Third, it takes another tuple of symbols whose first element points to the value accumulated
so far (and is also the result key of the computation); the second element is the key in the inner computation
to which the accumulated value should be assigned. Finally, it takes the inner computation.

An `AbortingComputation` wraps another computation and comes in several flavors. The `AbortIfNoResults`
and `AbortIfHasResults` abort a series of computations (a `SequentialComputation,` `IterativeComputation,`
`FoldingComputation,` or `MappingComputation`) if the inner computation has no results or has results,
respectively. The `AbortIf` allows for more sophisticated tests on the data map returned from the inner
computation, using a predicate expression specified as a Scala string.

### Using the jar

Using this library requires that the `SecurityConfiguration` trait be extended. For many purposes,
using the `DefaultSecurityConfiguration` object provides enough of a default implementation.

The security configuration operates at two levels:

First, by configuring the Java security manager, for which you will need to provide a Java policy file. (The
`DefaultSecurityConfiguration` sets the `java.security.policy` system property to `computation.security.policy`--the
name of the default policy file.) Several examples of policy files may be found in the `test/resources` folder.
Note that the computation engine will probably not be able to function without the following permission being set:

    grant {
      permission java.lang.reflect.ReflectPermission "suppressAccessChecks";
    };

Depending on the classes you are loading and the location of the jar files that contain them, you may also need
to grant file read permissions for those file locations. In general, it is probably easier and secure enough to
just establish blanket file read permission at the Java security manager level, and to prohibit computations from
reading files by relying on the fact that `java.io` is not a whitelisted package in the security configuration
for the Scala script engine (see below). To establish blanket file read permissions, add:

    grant {
      permission java.io.FilePermission "<<ALL FILES>>", "read";
    };

The second level of security configuration configures the Scala script engine. You can whitelist packages
whose classes are referenced within computations, and blacklist classes within those packages which
should not be referenced. The `SecurityConfiguration` trait provides a default whitelist of the packages 
most likely to be used by computations, and a blacklist of classes in those packages that have side effects. 
You can override these choices by extending the trait.

When using the computation engine, particularly during testing, it can sometimes become onerous to wait
for the computations to be recompiled each time an application or test suite is run. Since classes are
always compiled to the same location, you can use the Java system property `script.use.cached.classes`
to always skip compilation and to use the already-compiled classes.

The directory to which the classes are compiled can be controlled using the `script.classes` system property.
The value of this property should be a URI, not a simple path. The default directory to which classes
are compiled is under the directory indicated by the `java.io.tmpdir` system property, in the
`scala-script-engine-classes` subdirectory.

### Under the hood

Your Scala expression will be turned into code of the following form:

    $inputAssignments
    ($computationExpression : Option[Any]) match {
        case Some(value) => Map($resultKey -> value)
        case None => Map()
    }

where `$computationExpression` is a substitution variable representing your Scala expression, and
`$inputAssignments` is a string constructed by iterating over the identifier mapping you passed to the
constructor of the computation. This string is made up of statements of the following form:

    val $valWithType = domainFacts.get($domainKey).get.asInstanceOf[$theType]

where `$valWithType` represents a key in the identifier mapping and `$domainKey` represents the
corresponding value. The type `$theType` is obtained by taking the portion of `$valWithType` after
the colon character. These statements are what assign values to the free identifiers in your Scala
expression, which correspond to the portion of `$valWithType` preceding the colon.

The result of evaluating `$computationExpression` must be an `Option[Any]`. `None` signifies that
the computation did not produce any results, and the computation returns an empty map. If the
computation produces a result (wrapped in a `Some`), the computation returns a map containing
`$resultKey` as a key and the result as the value. This map later gets combined with the domain of
facts passed into the computation, and the combined map is returned as the result of the computation.

Computations are envisioned as stateless, with their results dependent only on the values passed in
as input. If you find yourself writing a computation that takes no inputs, consider a different
approach. E.g., instead of having a computation that adds a day to the current date, pass the current
date as input to the computation.

### Working with the source code

The gradle build tool is used to build the code. Gradle will also generate project files for IntelliJ IDEA
if the `gradle idea` command is run.

### Testing

Some thoughts on testing:
* Computations are intended to be stateless and to compute results deterministically as a function
  of their inputs alone. (Some work has gone into making the `DefaultSecurityConfiguration` exclude
  classes that manipulate state or have side effects.) Keeping computations stateless and functional
  also makes them easier to test.

* The input mapping of a computation establishes a contract that can and should be tested, namely,
  that the data map passed to the computation will contain certain keys that can be assigned to vals
  of specific types. The computation will throw an exception (which may not be propagated, depending
  on the flag) if this contract is not fulfilled. The caller of the computation should be tested
  rigorously to make sure it will always pass values of the correct types to the computation.

* Each step in a `SequentialConputation` imposes a contract on the output of the sequence that came
  before. Since each step in the sequence receives the entire original data map along with the
  results of the preceding computations, the results of the immediately preceding computation
  may not by themselves fulfill the contract. This also means that the first computation's input
  mapping does not necessarily specify the input contract for the entire sequence, since a computation
  later in the sequence might rely on a key in the input data that the first computation did not need.
  However, if a test can specify the shape of the data passed in to the first step in the sequence,
  then it may be possible to automatically generate tests (using e.g., ScalaCheck) to establish that
  contract conditions are always fulfilled for each step in the sequence.

### Future directions

Here are some things it would be useful to have:

* The ability to read computations from a database. That's up next on the roadmap.

* Tools for making it easy to test computations, using ScalaCheck to automate the generation of tests
  where possible. It would also be useful to have a testing module that could pull computations from the
  database and test them. This module could be included as a library in test projects where
  classpaths specific to the computations would be set up. Computation tests would live in this
  project as regular compiled code and would need to change as computations changed.

* A tool for deploying computations from one database to another, and for rolling back to a previous
  version if there is a problem.

* A visual tool for creating computations and inserting them into a database.

### Contribute to the project

We appreciate your desire to contribute. We have created a [Wiki](https://github.com/cyrusinnovation/computation-engine/wiki) to help you get your development environment up and running.  Please pay it forward and help us keep the [Wiki](https://github.com/cyrusinnovation/computation-engine/wiki) updated as the project changes.

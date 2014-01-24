# computation-engine

### A Scala library for evaluating sequences of computations written as Scala strings, on domains of arbitrary facts.

### Vision

The goal of this project is to allow arbitrary computations to be specified in a form that
can be manipulated without recompiling an application. Steps in the computation can be stored
in a database and read in at runtime, then changed without having to redeploy. This can be
useful, for example, for representing business rules that will be modified frequently.

Arbitrary computations require a generic representation of data and a powerful expression language
for manipulating that data. This library uses Scala as the expression language; strings representing
Scala expressions are evaluated in the Scala Script Engine sandbox to provide some safety. Data is
represented in generic form as Scala maps of `Symbol -> Any`.

### How to use it:

1. Instantiate your simple computations. A `SimpleComputation` has:
    * A package name - This is used to identify groups of computations that all belong to the same sequence
    of computations. This will also be the package name of the class compiled from the computation.
    * A name - An ordinary-language identifier for the computation.
    * A description - an ordinary-language description of the computation.
    * Imports - A list of fully-qualified classnames or other strings that each could be used in an
    "import" statement (without the "import" keyword).
    * The computation expression - A string containing a Scala expression. This expression should contain
    unbound identifiers that will be bound using the input map below.
    * An input mapping - A map of `String -> Symbol` whose keys are of the form `identifer:Type` where the
    identifiers are unbound in the computation expression and `Type` designates the type of the val that
    will be created using the identifer. The values of the input map are symbols designating the keys
    in the data map that will be bound to the specified vals.
    * A result key - A symbol that will be used as the key for the result of the computation. The output
    of the computation is a map containing the entire input data map as well as an entry for the result,
    with the result key as key and the computation's result as value.
    * A security configuration - An instance of the SecurityConfiguration trait indicating what packages
    are safe to load, what classes in those packages are unsafe to load, and where the Java security policy
    file for the current security manager is.
    * A logger - An instance of `com.cyrusinnovation.computation.util.Log`. A convenience case class
    `com.cyrusinnovation.computation.util.ComputationEngineLog` extends this trait and wraps an slf4j
    log passed to its constructor.
    * A "should propagate exceptions" flag - This flag indicates whether the computation should rethrow an
    exception if the expression fails to compile or if there is an exception when the computation is being
    applied.
2. Combine your computations.
    * A `SequentialComputation` is instantiated with a list of computations that will be the steps in the
    sequence.
    * An `IterativeComputation` takes a single computation over which it will iterate. It also takes a tuple
    of symbols that specifies the input: Its first element identifies the key in the data map whose value is
    the sequence to be iterated over, and the second element of the tuple is the key used to designate a single
    element of that sequence that will be inserted into the data map passed to the inner computation. Finally,
    the `IterativeComputation` takes a symbol identifying its result key. (The results of the inner computation
    are extracted using the inner computation's result key.)
    * An `AbortingComputation` wraps another computation and comes in several flavors. The `AbortIfNoResults`
    and `AbortIfHasResults` abort a series of computations (either a `SequentialComputation` or an
    `IterativeComputation`) if the inner computation has no results or has results, respectively. The `AbortIf`
    allows for more sophisticated tests on the data map returned from the inner computation, using a predicate
    expression specified as a Scala string.

3. Prepare your data. The data passed to the computation should be in the form of a Scala immutable `Map[Symbol, Any]`.

4. Call your computation's `compute` method.

5. Extract your results. The output of a computation is a `Map[Symbol, Any]` that includes the original data
passed to the computation, plus a map entry mapping the resultKey to the result of the computation. In a
`SequentialComputation` the output also contains map entries corresponding to the result keys to the
results of the intermediate computations. In an `IterativeComputation` the output does not contain the
results of the individual iterations; the only result is an entry with the computation's result key mapped
to the single result of all the iterations combined (returned as a List).

To get a better idea of how this all works, look at the tests for the various computation classes.

### Working with the source code

The gradle build tool is used to build the code. Gradle will also generate project files for IntelliJ IDEA
if the `gradle idea` command is run.

###


### Testing

Rules should be able to be read from a database. That should be up next.

The roadmap for the computation engine also includes developing tools for testing rules. Some thoughts:
    * The input mapping of a computation establishes a contract that can and should be tested, namely,
    that the data map passed to the computation will contain certain keys that can be assigned to vals
    of specific types. The computation will throw an exception (which may not be propagated, depending
    on the flag) if this contract is not fulfilled. The caller of the computation should be tested
    rigorously to make sure it will always pass values of the correct types to the computation.
    * Each step in a `SequentialConputation` imposes a contract on the output of the sequence that came
    before. This doesn't necessarily mean that the results of the immediately preceding computation
    will by themselves fulfill the contract, since each computation adds its own results to the
    data map it received. In addition, the first computation's input mapping does not necessarily
    specify the input contract for the entire sequence, since a computation further in the sequence
    might rely on some key in the input data that the first computation did not need. However, if a
    test can specify the shape of the data passed in to the first step in the sequence, then it should
    be possible to automate testing (using e.g., ScalaCheck) to establish that contract conditions
    are always fulfilled for each step in the sequence.
    * It would be useful to have a testing module that pulled rules from the database and tested
    them. This module could be included in a "testing project" in which classpaths specific to the
    computations could be set up; computation tests would live in this project as regular compiled
    code and would have to be changed when computations changed.

Further out, it would be useful to have a few more things:
    * A tool for deploying rules from one database to another, and for rolling back to a previous
    version if there is a problem.
    * A visual tool for creating rules and inserting them into a database.

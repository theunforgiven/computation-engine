
#
# Copyright 2014 Cyrus Innovation, LLC. Licensed under Apache license 2.0.
#

#TODO Move into Gradle
cd core/doc

scaladoc -doc-no-compile ../src/main/scala/com/cyrusinnovation/computation/util/*.scala ../src/main/scala/com/cyrusinnovation/computation/AbortingComputation.scala ../src/main/scala/com/cyrusinnovation/computation/CompoundComputation.scala ../src/main/scala/com/cyrusinnovation/computation/Computation.scala ../src/main/scala/com/cyrusinnovation/computation/Domain.scala ../src/main/scala/com/cyrusinnovation/computation/SecurityConfiguration.scala


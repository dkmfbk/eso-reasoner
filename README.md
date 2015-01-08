# README #

This is an addon to RDFpro that applies the rules of the ESO ontology and creates the resulting Situation(s).

### What is this repository for? ###

* `@esoreasoner` Performs reasoning according to the ESO ontology supplied
    [-i]          emits only inferences (default: emit also explicit statements)
    [-b BASE]     use BASE to resolve URIs in the ESO ontology files (default: empty)
    [-w]          rewrites BNodes in the ESO ontology file to avoid clashes
    FILE...       the file(s) of the ESO ontology
* `@reformattime` Perform filter on time entities, converting single time entities into integer values.
* `@filtertype` Perform filter on predicates, leaving only one kind of event for each

### Set up ###

* Download or checkout **RDFpro** from the [official website](http://fracor.bitbucket.org/rdfpro/) and expand it
* From the RDFpro folder, run `mvn package install -DskipTests`
* In the `rdfpro-dist/target/` folder, find `rdfpro-dist-x.x-SNAPSHOT.jar` file and expand it where you want to install RDFpro
* Download or checkout the **eso reasoner**
* From its folder, run `mvn package`
* Copy `eso-reasoner-x.x-SNAPSHOT.jar` from `target/` folder to the `lib/` folder in the RDFpro install dir
* If you run `rdfpro -h` you will see the list of available commands, included the new ones

### Examples of use ###

* `rdfpro @read /path/to/input.ttl @esoreasoner /path/to/ontology.owl @write /path/to/output.ttl`
* `rdfpro @read /path/to/input.ttl @reformattime @write /path/to/output.ttl`
* `rdfpro @read /path/to/input.ttl @filtertype @write /path/to/output.ttl`

### License ###

This software is released under the [WTFPL license, version 2.0](http://www.wtfpl.net/about/).
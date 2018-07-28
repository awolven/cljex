# cljex

An example clojure program.

## Installation

	$ git clone https://github.com/awolven/cljex.git
	$ cd cljex
	$ lein uberjar
	$ lein test

## Usage

Example usage:

    $ java -jar target/uberjar/cljex-0.1.0-SNAPSHOT-standalone.jar -i
    test/cljex/test-data-space-sep.txt -i
    test/cljex/test-data-comma-sep.txt -i
    test/cljex/test-data-pipe-sep.txt --port 9000 --list name

	$ java -jar target/uberjar/cljex-0.1.0-SNAPSHOT-standalone.jar --help

## Options

	[-h | --help]     get help on command
	[-l <TYPE> | --list <TYPE>]     type is one of "gender",
	"birthdate", or "name"
	[-p <PORT> | --port <PORT> ]     must be specified to start REST
	HTTP interface
	[-i <PATH> | --input <PATH>]     pathname to input file

## Examples

REST API:

In browers: http://localhost:9000/records/name

Also in browser:
http://localhost:9000/records/gender
http://localhost:9000/records/birthdate

	$ curl -X POST http://localhost:9000/records --data "Smith John male blue 1/15/1972"

### Bugs

Command line args must have argument pairs appropriately of startup
will fail.

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright © 2018 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

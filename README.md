# segera

## Intentions

This project intent as learning exercise in Java network programming. I want to create simple application and show how
it can be done in incremental steps starting from working directly with sockets. End goal is NIO based library capable
of working as zero memory footprint library.

To ensure my framework efficiency I need to measure performance, so I need performance counter library.

To make it more fun I decide I won't use any libraries except selected few (logging, lombok, test).

### Versions

* V1 - Initial version
* V2 - Separate code into infrastructure and business layers.
* V3 - Extend protocol. Introduce multiple message types and protocol version.
* V4 - Switch server to NIO.
* V5 - Separate serialization logic and server logic.
* V6 - Model generator: parser.
* V7 - Model generator: DTO generator.
* V8 - Stop serializer dependency on message size
* V9 - Model generator: Flyweight interfaces generator
* V10 - Switch server to Flyweight interfaces
* V11 - Model generator: generate Flyweight implementations
* V12 - Create NIO based client
* V13 - Performance counter: Create in process library.
* V14 - Connector: Fix buffer overflow problem 
the reason why i choose my implementation:
- can't get the dependencies setup for the existing paxos implementations
- i don't like tencent (biased opinion). tho i can't set it up anyways
- don't want to write build scripts
- have no idea how to test code on there
- my code is already working pretty well
- i'm pretty good at automated testing
- MOST IMPORTANTLY: lazy

parameters that might affect runtime performance + how much it affect:
- number of nodes in the network - a lot
  - 2 concurrent nodes, 10 runs
  - 9 nodes: `30ms` success, `15ms` fail
  - 99 nodes: `600ms` success, `400ms` fail


how to run:
```bash
# build
javac -d bin *.java

java -cp bin Test [args0] [args1] [args2] [args3] ([optional - args4] [optional args[5]])
# args0: set to 't' so nodes will print proposal result. anything else it'll not print
# args1: number of nodes in the network
# args2: number of nodeds propose at the same time
# args3: number of runs to try and average out, recommend 10 runs

```
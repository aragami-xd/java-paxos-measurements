the reason why i choose my implementation:
- can't get the dependencies setup for the existing paxos implementations
- i don't like tencent (biased opinion). tho i can't set it up anyways
- don't want to write build scripts
- have no idea how to test code on there
- my code is already working pretty well
- i'm pretty good at automated testing
- MOST IMPORTANTLY: lazy

how to run:
```bash
# build
javac -d bin *.java

# test no failures
java -cp bin TestNoFailtures [args0] [args1] [args2] [args3]
# args0: number of nodes in the network
# args1: number of nodeds propose at the same time
# args2: number of runs to try and average out, recommend 10 runs
# args3: set to 't' so nodes will print proposal result

# test failures
```
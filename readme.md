NOTE: this is run on a "slightly modified" version from the previous assignment, due to:
- bug fixes
- changes to how offline works: the assignment will send a delayed response, but this implementation will ignore the message all together, since testing with too many nodes, too many runs will create too many threads, crashing the program
- remove all println and stuff
- add message count to result (doesn't affect the logic)

<br>

the reason why i choose my implementation:
- can't get the dependencies setup for the existing paxos implementations
- i don't like tencent (biased opinion). tho i can't set it up anyways
- don't want to write build scripts
- have no idea how to test code on there
- my code is already working pretty well
- i'm pretty good at automated testing
- MOST IMPORTANTLY: lazy

<br>

parameters that might affect runtime performance:
- number of nodes in the network:
  - impact: A LOT
  - e.g. 2 concurrent nodes, 10 runs
  - 9 nodes: `30ms` success, `15ms` fail
  - 99 nodes: `600ms` success, `400ms` fail
- number of nodes propose concurrently:
  - impact: not much, BUT it can skew the average rejected proposal time
  - the more nodes propose concurrently, the higher average rejected proposal time is
  - usually succeeded proposal takes longer, but if too many nodes propose at a time rejected proposals eventually pass the succeeded proposal (due to processing, maybe?)
- number of node failed:
  - impact: A LOT
  - timeout is set to 1 SECOND for each phase
  - add at least 2-3 seconds to succeeded proposal & 1-2 seconds to rejected proposals

<br>

how to run:
```bash
# build
javac -d bin *.java

java -cp bin Test [args0] [args1] [args2] ([optional args3])
# args0 - default = 9  : number of nodes in the network
# args1 - default = 2  : number of nodes propose at the same time
# args2 - default = 10 : number of runs to try and average out, recommend 10 runs
# args3 - default = 0  : number of nodes failed (-> 0 = no failure mode)
# NOTE: i will not check for invalid input
```
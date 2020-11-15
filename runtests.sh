#build
javac -d bin *.java
#java -cp bin Test [minNodes] [maxNodes] [minConcurent] [maxConcurrent] [minFailures] [maxFailures] [minTimeout] [maxTimeout] [runsToAggregateOver][outputFilename]
java -cp bin Test 3 20 1 1 0 0 100 100 20 Outputs/3-20Nodes0Fails.csv
java -cp bin Test 3 20 1 1 1 1 100 100 20 Outputs/3-20Nodes1Fail.csv
java -cp bin Test 3 20 1 1 9 9 100 100 20 Outputs/3-20Nodes9Fails.csv
java -cp bin Test 3 20 1 1 20 20 100 100 20 Outputs/3-20Nodes20Fails.csv
java -cp bin Test 20 20 1 20 0 0 100 100 20 Outputs/1-20Concurrent0Fails.csv
java -cp bin Test 20 20 1 20 1 1 100 100 20 Outputs/1-20Concurrent1Fail.csv
java -cp bin Test 20 20 1 20 9 9 100 100 20 Outputs/1-20Concurrent9Fails.csv
java -cp bin Test 20 20 1 20 20 20 100 100 20 Outputs/1-20Concurrent20Fails.csv

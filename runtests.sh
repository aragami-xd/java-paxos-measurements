#build
javac -d bin *.java
#java -cp bin Test [minNodes] [maxNodes] [minConcurent] [maxConcurrent] [percentFailures (between 0 and 1)] [timeout] [runsToAggregateOver][outputFilename]
java -cp bin Test 3 20 1 1 0 100 20 Outputs/3-20Nodes0Fails.csv
java -cp bin Test 3 20 1 1 0.1 100 20 Outputs/3-20Nodes10percentFails.csv
java -cp bin Test 3 20 1 1 0.5 100 20 Outputs/3-20Nodes50percentFails.csv
java -cp bin Test 3 20 1 1 0.9 100 20 Outputs/3-20Nodes90percentFails.csv
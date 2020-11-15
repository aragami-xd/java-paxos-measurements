import java.util.ArrayList;

public class TestRunner {

	public static void main(String[] args) {
		// new Test(Integer.parseInt(args[0]), Integer.parseInt(args[1]),
		// Integer.parseInt(args[2]), Integer.parseInt(args[3]));
		// Create a CSV with results
		ArrayList<String> lines = new ArrayList<String>();

		// The header of the CSV. I wrote it like this because it becomes this really
		// long single line string that is hard to read otherwise
		String header = Util.generateCSVHeader();

		lines.add(header);

		// variables
		int minNumNodes = Integer.parseInt(args[0]);
		int maxNumNodes = Integer.parseInt(args[1]);

		int minConcurrent = Integer.parseInt(args[2]);
		int maxConcurrent = Integer.parseInt(args[3]);

		int minFailures = Integer.parseInt(args[4]);
		int maxFailures = Integer.parseInt(args[5]);

		int timeout = Integer.parseInt(args[6]);

		int runs = Integer.parseInt(args[7]);
		String output = args[8];

		// Usually the first test takes wayy longer and then the rest take way less
		// time for some reason, probably low level computer caching related
		// To account for this, a dummy-test with the minimum required parameters is
		// being run before the actual tests
		new Test(3, 1, 1, 0, 250);

		for (int numNodes = minNumNodes; numNodes <= maxNumNodes; numNodes++) {
			for (int concurrentProposals = Util.min(minConcurrent, numNodes); concurrentProposals <= Util.min(numNodes,
					maxConcurrent); concurrentProposals++) {
				for (int failures = Util.min(numNodes, minFailures); failures <= Util.min(numNodes, maxFailures); failures++) {
					Test results = new Test(numNodes, concurrentProposals, runs, failures, timeout);
					String row = Util.generateDatapoint(results);
					lines.add(row);
				}
			}
		}

		Util.writeCSV(lines, output);

		System.exit(0);
	}
}

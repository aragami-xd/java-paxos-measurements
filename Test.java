import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

public class Test {
	private Node[] nodes;

	private Result[] results;
	private Thread[] threads;

	private long[] successNodes; // timers for successful runs
	private long successTotal = 0; // total time for all successful runs
	private int successRuns; // number of successful runs (NOTE: due to concurrent proposal, some runs might
														// have all nodes failed)

	private long[] failedNodes; // timers for failed nodes each run. This is not symmetric to successNodes
															// though it may look like it is
	private long[] failedNodesAverage; // average time for all failed nodes each run. symmetric with successNodes
	private long failedTotal = 0; // total time for all average failed nodes above

	private int messages = 0;
	// This one is zeroed after each run
	private int messagesCounter2 = 0;

	private synchronized void addMessages(int count) {
		messages += count;
		messagesCounter2 += count;
	}

	// Does not count zero and negative values, which is a requirement specific to
	// these tests
	private double calculateSD(long[] numArray, boolean debug) {
		long sum = 0;
		// Some array results may be 0 and not count, as per N
		int N = 0;
		for (long num : numArray) {
			if (num > 0)
				N++;
		}

		for (long num : numArray) {
			if (num <= 0)
				continue;
			sum += num;
			if (debug)
				System.out.println(num);
		}

		double mean = sum / ((double) N);

		if (debug)
			System.out.println("mean: " + mean);

		double variance = 0.0;

		for (long num : numArray) {
			if (num <= 0)
				continue;

			variance += Math.pow(((double) num) - mean, 2) / ((double) N);
		}
		return Math.sqrt(variance);
	}

	public double proposalSuccessTime = 0;
	public double proposalSuccessTimeStdev = 0;

	public double proposalRejectTime = 0;
	public double proposalRejectTimeStdev = 0;

	public double averageMessages = 0;
	public double averageMessagesStdev = 0;

	public int numberOfFailures = 0;
	public int numberOfSuccesses = 0;

	private long[] numMessages;
	public int totalMessages = 0;

	/**
	 * run test
	 * 
	 * @param nodeCount          how many nodes in the network
	 * @param concurrentProposal how many nodes propose at the same time
	 * @param runs               how many runs (recommend 10 runs)
	 * @param failures           how many acceptor nodes fail. if <= 0, no failures
	 * @param timeout            the timeout parameter for the nodes in the network
	 *                           (in milliseconds)
	 */
	public Test(int nodeCount, int concurrentProposal, int runs, int failures, int timeout) {
		System.out.println("config");
		System.out.println("nodes:                " + nodeCount);
		System.out.println("concurrent proposals: " + concurrentProposal);
		System.out.println("runs:                 " + runs);
		System.out.println("no. failed nodes:     " + failures + "\n");

		// create nodes
		nodes = new Node[nodeCount];
		for (int i = 0; i < nodes.length; i++) {
			final int t = i;

			new Thread(() -> {
				nodes[t] = new Node(nodes.length, t, timeout);
				nodes[t].start();
			}).start();
		}

		// initialize arrays
		results = new Result[concurrentProposal];
		threads = new Thread[concurrentProposal];

		successNodes = new long[runs];
		Arrays.fill(successNodes, 0);

		successRuns = runs;

		failedNodes = new long[concurrentProposal];
		Arrays.fill(failedNodes, 0);
		failedNodesAverage = new long[runs];

		numMessages = new long[runs];
		Arrays.fill(numMessages, 0);

		// a "hack" for scenarios where nodes constructors are not yet completed (for
		// the few final threads)
		try {
			Thread.sleep(200);
		} catch (Exception e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}

		// if acceptor failure, put them offline
		if (failures > 0) {
			for (int i = 1; i <= failures; i++) {
				final int index = i;
				new Thread(() -> {
					nodes[nodes.length - index].goOffline(100000);
				}).start();
			}
		}

		// nodes propose
		for (int i = 0; i < runs; i++) {
			System.out.println("run " + i);
			final int i_ = i;

			// new thread for each concurrent proposal
			for (int t = 0; t < concurrentProposal; t++) {
				final int t_ = t;

				threads[t_] = new Thread(() -> {
					long start = System.nanoTime() / 1000000;
					results[t_] = nodes[t_].propose(0, 0, 0);
					long end = System.nanoTime() / 1000000;

					addMessages(results[t_].messages);

					if (results[t_].p3) { // if Result.p3 == true, proposal successful (phase 3 successful)
						successNodes[i_] = end - start;
					} else {
						failedNodes[t_] = end - start;
					}
				});
				threads[t_].start();
			}

			// wait for all threads to join and calculate the result
			long failedNodesTotal = 0;
			for (int t = 0; t < concurrentProposal; t++) {
				try {
					threads[t].join();
				} catch (Exception e) {
					System.err.println(e.toString());
					e.printStackTrace();
				}
				failedNodesTotal += failedNodes[t];
			}

			numMessages[i_] = messagesCounter2;
			messagesCounter2 = 0;

			if (successNodes[i_] > 0)
				successTotal += successNodes[i_];
			else
				successRuns--;

			if (concurrentProposal > 1) {
				failedNodesAverage[i] = failedNodesTotal / (concurrentProposal - 1); // 1 successful node
				failedTotal += failedNodesAverage[i];
			}
		}

		// stop all threads
		for (int i = 0; i < nodes.length; i++)
			nodes[i].stop();

		// print result
		System.out.println("\nruntime result for each run (in ms): ");
		System.out.println("(NOTE: if success run = 0, no nodes successfully proposed)\n");
		for (int i = 0; i < runs; i++)
			System.out.println("success: " + (successNodes[i]) + " -- average failed: " + (failedNodesAverage[i]));

		proposalSuccessTime = 0;
		proposalSuccessTimeStdev = 0;
		if (successRuns > 0) {
			proposalSuccessTime = successTotal / (successRuns);
			proposalSuccessTimeStdev = calculateSD(successNodes, false);
		}

		int failedRuns = (runs - successRuns);

		proposalRejectTime = 0;
		proposalRejectTimeStdev = 0;
		if (failedRuns > 0) {
			proposalRejectTime = failedTotal / (failedRuns);
			proposalRejectTimeStdev = calculateSD(failedNodesAverage, false);
		}

		totalMessages = messages;
		averageMessages = messages / ((double) runs);
		averageMessagesStdev = calculateSD(numMessages, false);

		numberOfFailures = failedRuns;
		numberOfSuccesses = successRuns;

		System.out.println("\naverage successful proposal time: " + proposalSuccessTime + "(ms)");
		System.out.println("average rejected proposal time:   " + proposalRejectTime + "(ms)\n");

		System.out.println("total number of messages (for all runs): " + messages);
		System.out.println("average number of message (per run):     " + messages / runs);
	}

	static int min(int a, int b) {
		return (a < b ? a : b);
	}

	static void writeCSV(ArrayList<String> lines, String dir) {
		try {
			File file = new File(dir);
			File parent = file.getParentFile();
			if (parent != null) {
				parent.mkdirs();
			}
			PrintWriter writer = new PrintWriter(new FileOutputStream(file));

			for (String line : lines) {
				writer.println(line);
			}

			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// new Test(Integer.parseInt(args[0]), Integer.parseInt(args[1]),
		// Integer.parseInt(args[2]), Integer.parseInt(args[3]));
		// Create a CSV with results
		ArrayList<String> lines = new ArrayList<String>();

		// The header of the CSV. I wrote it like this because it becomes this really
		// long single line string that is hard to read otherwise
		String header = "" + "Nodes," + "Timeout used," + "Concurrent Proposals," + "Failures," + "Runs," + "Successes,"
				+ "Failures," + "proposal success time average," + "proposal success time stdev,"
				+ "proposal fail time average," + "proposal fail time stdev," + "messages per run average,"
				+ "messages per run stdev";

		lines.add(header);

		// variables
		int minNumNodes = Integer.parseInt(args[0]);
		int maxNumNodes = Integer.parseInt(args[1]);

		int minConcurrent = Integer.parseInt(args[2]);
		int maxConcurrent = Integer.parseInt(args[3]);

		int minFailures = Integer.parseInt(args[4]);
		int maxFailures = Integer.parseInt(args[5]);

		int minTimeout = Integer.parseInt(args[6]);
		int maxTimeout = Integer.parseInt(args[7]);

		int runs = Integer.parseInt(args[8]);
		String output = args[9];

		// Usually the first test takes wayy longer and then the rest take way less
		// time for some reason, probably low level computer caching related
		// To account for this, a dummy-test with the minimum required parameters is
		// being run before the actual tests
		new Test(3, 1, 1, 0, 250);

		for (int timeout = minTimeout; timeout <= maxTimeout; timeout++) {
			for (int numNodes = minNumNodes; numNodes <= maxNumNodes; numNodes++) {
				for (int concurrentProposals = min(minConcurrent, numNodes); concurrentProposals <= min(numNodes,
						maxConcurrent); concurrentProposals++) {
					for (int failures = min(numNodes, minFailures); failures <= min(numNodes, maxFailures); failures++) {
						Test results = new Test(numNodes, concurrentProposals, runs, failures, timeout);

						// Rows in the CSV we're going to print. should be in the same as the header
						String row = "" + numNodes + "," + timeout + "," + concurrentProposals + "," + failures + "," + runs + ","
								+ results.numberOfSuccesses + "," + results.numberOfFailures + "," + results.proposalSuccessTime + ","
								+ results.proposalSuccessTimeStdev + "," + results.proposalRejectTime + ","
								+ results.proposalRejectTimeStdev + "," + results.averageMessages + "," + results.averageMessagesStdev;

						lines.add(row);
					}
				}
			}
		}

		writeCSV(lines, output);

		System.exit(0);
	}
}
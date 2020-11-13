public class TestNoFailures {
	private Node[] nodes;

	private Result[] results;
	private Thread[] threads;

	private long[] successNodes; // timers for successful runs
	private long successTotal = 0; // total time for all successful runs
	private int successRuns; // number of successful runs (NOTE: due to concurrent proposal, some runs might have all nodes failed)

	private long[] failedNodes; // timers for failed nodes each run
	private long[] failedNodesAverage; // average time for all failed nodes each run
	private long failedTotal = 0; // total time for all average failed nodes above

	private int messages = 0;

	private synchronized void addMessages(int count) {
		messages += count;
	}

	/**
	 * run test	
	 * @param nodeCount how many nodes in the network
	 * @param concurrentProposal how many nodes propose at the same time
	 * @param runs how many runs (recommend 10 runs)
	 */
	public TestNoFailures(int nodeCount, int concurrentProposal, int runs) {
		System.out.println("config");
		System.out.println("nodes:                " + nodeCount);
		System.out.println("concurrent proposals: " + concurrentProposal);
		System.out.println("runs:                 " + runs + "\n");

		// create nodes
		nodes = new Node[nodeCount];
		for (int i = 0; i < nodes.length; i++) {
			final int t = i;

			new Thread(() -> {
				nodes[t] = new Node(nodes.length, t);
				nodes[t].start();
			}).start();
		}

		// initialize arrays
		results = new Result[concurrentProposal];
		threads = new Thread[concurrentProposal];

		successNodes = new long[runs];
		successRuns = runs;

		failedNodes = new long[concurrentProposal];
		failedNodesAverage = new long[runs];

		try {
			Thread.sleep(500);
		} catch (Exception e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}

		// nodes propose
		for (int i = 0; i < runs; i++) {
			System.out.println("run " + i);
			final int i_ = i;

			// new thread for each concurrent proposal
			for (int t = 0; t < concurrentProposal; t++) {
				final int t_ = t;

				threads[t_] = new Thread(() -> {
					long start = System.nanoTime();
					results[t_] = nodes[t_].propose(0, 0, 0);
					long end = System.nanoTime();

					if (results[t_].p3) { // if Result.p3 == true, proposal successful (phase 3 successful)
						successNodes[i_] = end - start;
						addMessages(6); // 3 phases = 6 messages
					} else {
						failedNodes[t_] = end - start;
						addMessages(2); // it'll fail in phase 1 (for no failure), i wrote the code, i know it
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

			if (successNodes[i_] > 0)
				successTotal += successNodes[i_];
			else
				successRuns--;

			if (concurrentProposal > 1) {
				failedNodesAverage[i] = failedNodesTotal / (concurrentProposal - 1);
				failedTotal += failedNodesAverage[i]; // 1 successful node
			}
		}

		// stop all threads
		for (int i = 0; i < nodes.length; i++)
			nodes[i].stop();

		// print result
		System.out.println("\nruntime result for each run (in nanoseconds): ");
		System.out.println("(NOTE: if success run = 0, no nodes successfully proposed)\n");
		for (int i = 0; i < runs; i++)
			System.out.println("success: " + successNodes[i] + " -- average failed: " + failedNodesAverage[i]);

		System.out.println("\naverage success time: " + successTotal / (successRuns * 1000000) + "(ms)");
		System.out.println("average failed time:  " + failedTotal / (runs * 1000000) + "(ms)\n");

		System.out.println("total number of messages (for all runs): " + messages);
		System.out.println("average number of message (per run):     " + messages / runs);
	}

	public static void main(String[] args) {
		if (args.length > 3 && args[2].equals("t")) // if print node running result
			Node.PRINT = true;

		new TestNoFailures(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
	}
}
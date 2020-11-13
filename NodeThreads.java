
// file contains all node processing threads
import java.net.*;

/**
 * proposer threads, sending data to each acceptor and read it's data
 * 
 * before thread joins, it'll set the return value to the corresponding index in the main node's result array
 * 
 * e.g. boolean[9] result -> thread joining sets result[0] = true;
 */

// phase 1
class ProposerPhase1Thread extends Thread {
	private Socket socket;
	private Node node;
	private int location;
	private int index;

	/**
	 * create new proposal thread
	 * @param node main node running thread
	 * @param location port of the acceptor node to send to
	 * @param index index of the result array
	 */
	ProposerPhase1Thread(Node node, int location, int index) {
		this.node = node;
		this.location = location;
		this.index = index;
	}

	/**
	 * <p> run thread - scenarios:
	 * <p> * no errors occurred: set promise = socket data
	 * <p> * socket timeout: set promise = rejected
	 * <p> * else (exception / invalid socket data): leave as null
	 */
	public void run() {
		try {
			socket = new Socket("localhost", location);
			socket.setSoTimeout(1000);

			// send prepare request & wait for resposne
			node.write(socket, node.proposalIdentifier);
			Object data = node.read(socket);

			// response object should be the promise. else, error occured
			if (data instanceof AcceptorResponse)
				node.p1Return.set(index, (AcceptorResponse) data);
			else
				System.err.println("index " + index + " - p2: data is NOT instanceof AcceptorResponse: "
						+ data.getClass().getName());

		} catch (SocketTimeoutException e) { // ignore upon timeout
		} catch (Exception e) {
		} finally {
			try {
				socket.close();
			} catch (Exception e) {
			}
		}
	}
}

// phase 2
class ProposerPhase2Thread extends Thread {
	private Socket socket;
	private Node node;
	private int location;
	private int index;

	/**
	 * create new proposal thread
	 * @param node main node running thread
	 * @param location port of the acceptor node to send to
	 * @param index index of the result array
	 */
	ProposerPhase2Thread(Node node, int location, int index) {
		this.node = node;
		this.location = location;
		this.index = index;
	}

	public void run() {
		try {
			socket = new Socket("localhost", location);
			socket.setSoTimeout(1000);

			// send accept request and wait for response
			node.write(socket, new AcceptRequest(node.proposalIdentifier, node.proposalValue));
			Object data = node.read(socket);

			// response object should be boolean (accept the request or not). else, error occured
			if (data instanceof AcceptorResponse)
				node.p2Return.set(index, (AcceptorResponse) data);
			else
				System.err.println("index " + index + " - p2: data is NOT instanceof AcceptorResponse: "
						+ data.getClass().getName());

		} catch (SocketTimeoutException e) { // ignore upon timeout
		} catch (Exception e) {
		} finally {
			try {
				socket.close();
			} catch (Exception e) {
			}
		}
	}
}

// phase 3
class ProposerPhase3Thread extends Thread {
	private Socket socket;
	private Node node;
	private int location;
	private int index;

	/**
	 * create new proposal thread
	 * @param node main node running thread
	 * @param location port of the acceptor node to send to
	 */
	ProposerPhase3Thread(Node node, int location, int index) {
		this.node = node;
		this.location = location;
		this.index = index;
	}

	public void run() {
		try {
			socket = new Socket("localhost", location);
			socket.setSoTimeout(1000);

			node.write(socket, true);
			Object data = node.read(socket);

			if (data instanceof Boolean)
				node.p3Return.set(index, (Boolean) data);
			else
				System.err.println(
						"index " + index + " - p3: data is NOT instanceof Boolean: " + data.getClass().getName());

		} catch (SocketTimeoutException e) {
		} catch (Exception e) {
		} finally {
			try {
				socket.close();
			} catch (Exception e) {
			}
		}
	}
}

/**
 * acceptor thread
 */
class AcceptorThread extends Thread {
	private Socket socket;
	private Node node;

	/**
	 * create new proposal thread
	 * @param node main node running thread
	 * @param socket 
	 */
	AcceptorThread(Node node, Socket socket) {
		this.node = node;
		this.socket = socket;
	}

	public void run() {
		/**
		 * this is the part that got changed: instead of `while (offline) do nothing`
		 * do `if (offline) ignore`
		 * 
		 * this is done because if many runs are performed, the node will create too many threads and cause the system to lag out
		 */
		
		// while (!node.online)
		// 	;

		if (!node.online)
			return;

		Object data = null;
		try {
			data = node.read(socket);

			if (data instanceof Integer)
				node.phase1b(socket, (Integer) data);
			else if (data instanceof AcceptRequest)
				node.phase2b(socket, (AcceptRequest) data);
			else if (data instanceof Boolean)
				node.phase3b(socket);
			else
			System.err.println(
					"data is NOT instanceof Integer, AcceptRequest or Boolean: " + data.getClass().getName());

		} catch (SocketTimeoutException e) {
		} catch (Exception e) {
		} finally {
			try {
				socket.close();
			} catch (Exception e) {
			}
		}
	}
}
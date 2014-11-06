import java.io.PrintStream;
import java.util.HashMap;

public class Bellman_Ford_Matrix {
	private final String myName;
	private final ForwardingTable forwardingTable;

	// Couple = (Destination, Relay)
	private HashMap<Couple, Integer> matrix;

	Bellman_Ford_Matrix(String name, ForwardingTable table) {
		myName = name;
		forwardingTable = table;
		matrix = new HashMap<Couple, Integer>();
	}

	/**
	 * Takes as a new information the existence of a route to a specified
	 * {@code destination} at the specified {@code distance} via a specified
	 * {@code relay}. Cases of a new destination or a new relay are handled
	 * accordingly, adding entries as required. When a route to the given
	 * destination via the given relay was already known, the distance is
	 * updated with the new given value. <br/>
	 * Next, one analyzes whether this changes the routing information. If so,
	 * the routing table is updated and the method returns {@code true}.
	 * Otherwise, the routing table is unchanged and the function returns
	 * {@code false}. The returned value should then be checked for deciding of
	 * the sending of a DV.
	 * 
	 * @param destination
	 *            the name of the destination node
	 * @param relay
	 *            the name of the relay that realizes the route
	 * @param newMetric
	 *            the distance for the given destination
	 * @return {@code true} when sending a DV is required, and {@code false}
	 *         otherwise
	 */
	public boolean updateDistance(String destination, String relay, int newMetric) {
		assert (!destination.equals(relay));
		Couple c = getCouple(destination, relay);
		int oldMetric = RouteEntry.INFINITY;
		if (c == null) c = new Couple(destination, relay);
		else oldMetric = matrix.get(c);

		matrix.put(c, newMetric);
		RouteEntry re = forwardingTable.get(destination);
		if (re == null) {
			String newRelay = getBestRelay(destination);
			forwardingTable.put(new RouteEntry(destination, newRelay, matrix.get(getCouple(destination, newRelay))));
			return true;
		} else if (newMetric < oldMetric) {
			int oldBestMetric = re.getMetrics();
			// If the new entry gives a better relay to get to {@code
			// destination}
			if (oldBestMetric > newMetric) {
				forwardingTable.put(new RouteEntry(re.getDestination(), relay, newMetric));
				return true;
			}
		} else if (newMetric > oldMetric) {
			String oldRelay = re.getRelay();
			// If the new entry cancel the best route we had before to get
			// to {@code destination}
			if (oldRelay == relay) {
				String newRelay = getBestRelay(destination);
				forwardingTable
						.put(new RouteEntry(destination, newRelay, matrix.get(getCouple(destination, newRelay))));
				return true;
			}
		}
		return false;
	}

	/**
	 * Dumps the whole content of this structure onto the specified
	 * {@code PrintStream}. The display order and format are not specified.
	 * 
	 * @param out
	 *            the stream on which the content is printed
	 */
	public void dump(PrintStream out) {
		for (Couple c : matrix.keySet())
			out.print("Route to " + c.destination + " via " + c.relay + " has a metric of " + matrix.get(c) + ".");
	}

	public Couple getCouple(String dest, String rel) {
		for (Couple c : matrix.keySet())
			if (c.destination == dest && c.relay == rel) return c;
		return null;
	}

	/*
	 * Use the matrix to get the best relay to get to destination {@code dest}
	 */
	public String getBestRelay(String dest) {
		int minMetric = -1;
		String bestRelay = null;
		for (Couple c : matrix.keySet()) {
			if (c.destination == dest) {
				int potMetric = matrix.get(c);
				if (potMetric < minMetric || minMetric == -1) {
					minMetric = potMetric;
					bestRelay = c.relay;
				}
			}
		}
		return bestRelay;
	}
}

class Couple {
	public String destination;
	public String relay;

	public Couple(String d, String r) {
		destination = d;
		relay = r;
	}
}

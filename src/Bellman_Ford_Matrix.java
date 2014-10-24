import java.io.PrintStream;

public class Bellman_Ford_Matrix {
  private final String myName;
  private final ForwardingTable forwardingTable;
  // MORE INTERNAL STORAGE REQUIRED

  Bellman_Ford_Matrix(String name, ForwardingTable table) {
    myName = name;
    forwardingTable = table;
    // TO BE COMPLETED
  }

  /**
   * Takes as a new information the existence of a route to a specified
   * {@code destination} at the specified {@code distance} via a specified
   * {@code relay}. Cases of a new destination or a new relay are handled
   * accordingly, adding entries as required. When a route to the given
   * destination via the given relay was already known, the distance is updated
   * with the new given value. <br/>
   * Next, one analyzes whether this changes the routing information. If so, the
   * routing table is updated and the method returns {@code true}. Otherwise,
   * the routing table is unchanged and the function returns {@code false}. The
   * returned value should then be checked for deciding of the sending of a DV.
   * 
   * @param destination
   *          the name of the destination node
   * @param relay
   *          the name of the relay that realizes the route
   * @param distance
   *          the distance for the given destination
   * @return {@code true} when sending a DV is required, and {@code false}
   *         otherwise
   */
  public boolean updateDistance(String destination, String relay, int distance) {
	  boolean res = false;
	  if (forwardingTable.get(destination) != null)
	  {
		  RouteEntry re = forwardingTable.get(destination);
		  if (re.getMetrics() != distance || !re.getRelay().equals(relay))
			  res = true;
	  }
	  forwardingTable.put(new RouteEntry(destination,relay, distance));
	  return res;
    // TO BE COMPLETED
    // retrieve the existing entry
    // does it change something?
    // does it give a better route?
    // does it downgrade the previous best route?
    // must seek for an alternate route?
    // touch ForwardingTable accordingly
  }

  /**
   * Dumps the whole content of this structure onto the specified
   * {@code PrintStream}. The display order and format are not specified.
   * 
   * @param out
   *          the stream on which the content is printed
   */
  public void dump(PrintStream out) {
    // TO BE COMPLETED
  }

}

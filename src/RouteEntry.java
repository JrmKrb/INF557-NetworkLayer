/**
 * A {@code RouteEntry} is intended as a record for a elementary routing
 * information.
 * 
 * Such an object keeps together a destination, a relay through which this
 * destination is reachable, and a metrics for the corresponding path.
 */

public class RouteEntry {
  private final String destination; // the name of the destination
  private final String relay; // the name of a relay toward this destination
  private final int metrics; // the cost of the path toward this destination

  public static final int INFINITY = 16; // as for RIP

  /**
   * A {@code RouteEntry} object is build each time a path to a destination
   * through a given relay is learned.
   * 
   * @param destinationName
   *          the name of the destination
   * @param relayName
   *          the name of the relay
   * @param d
   *          the metrics for this path
   */
  public RouteEntry(String destinationName, String relayName, int d) {
    this.destination = destinationName;
    this.relay = relayName;
    this.metrics = d;
  }

  /**
   * Returns the destination registered into this {@code RouteEntry}.
   * 
   * @return the registered destination
   */
  public String getDestination() {
    return destination;
  }

  /**
   * Returns the relay registered into this {@code RouteEntry}.
   * 
   * @return the registered relay
   */
  public String getRelay() {
    return relay;
  }

  /**
   * Returns the metrics registered into this {@code RouteEntry}.
   * 
   * @return the registered metric
   */
  public int getMetrics() {
    return metrics;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return this.destination + " at metrics "
        + (this.metrics < INFINITY ? "" + this.metrics : "INF") + " via "
        + this.relay;
  }

}

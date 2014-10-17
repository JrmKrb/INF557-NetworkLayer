import link.Link;

/**
 * A {@code LinkEntry} is intended as a record into a link state table.
 * 
 * Such an object is associated to each known direct neighbor and it keeps
 * together the name of this neighbor and through which link it is directly
 * reachable. A {@code LinkEntry} also implements a down-counter to help the
 * detection of which neighbors are off.
 */

public class LinkEntry {
  private final String target; // the name of a neighbor
  private final Link link; // the link through which it can be reached
  private int counter; // the current counter value

  /**
   * A {@code LinkEntry} object is build each time a new neighbor is discovered
   * on a given link.
   * 
   * @param neighborName
   *          the name of the new neighbor
   * @param discoveryLink
   *          the link through which it is now supposed reachable
   * @param initialCount
   *          the initial value for the down-counter
   */
  public LinkEntry(String neighborName, Link discoveryLink, int initialCount) {
    this.target = neighborName;
    this.link = discoveryLink;
    this.counter = initialCount;
  }

  /**
   * Returns the neighbor registered into this {@code LinkEntry}.
   * 
   * @return the name of the neighbor
   */
  public String getNeighbor() {
    return this.target;
  }

  /**
   * Returns the link registered into this {@code LinkEntry}.
   * 
   * @return the link to reach the associated neighbor
   */
  public Link getLink() {
    return this.link;
  }

  /**
   * Returns the current value of the down-counter for this {@code LinkEntry}.
   * 
   * @return the count-down value
   */
  public int getCounter() {
    return this.counter;

  }

  /**
   * Decreases by one the current value of the down-counter for this
   * {@code LinkEntry}.
   * 
   */
  public void decreaseCounter() {
    --this.counter;
  }

  /**
   * Sets the specified value as the current value of the down-counter
   * associated to this {@code LinkEntry}.
   * 
   * @param newCount
   *          the new value for the down-counter
   */
  public void setCounter(int newCount) {
    this.counter = newCount;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return this.target + " on " + this.link + " (" + this.counter + ")";
  }

}

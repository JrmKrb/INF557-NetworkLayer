package common;

import packets.Packet;

/**
 * This interface declares the basic functions of any layer. According to the
 * implemented protocol, the {@link #send send} -- {@link #receive receive} pair
 * should make the best effort to put a message available at destination. <br/>
 * The {@link #close close} method should also, before finally closing, make the
 * best effort to send all the enqueued messages. <br/>
 * The {@link #deliverTo deliverTo} method is to be used for establishing the
 * upward path for the incoming packets.
 * 
 * @see link.Link
 * @see TopLayer
 * 
 * @author Thomas Clausen
 * @author Philippe Chassignet
 * @author INF557, DIX, © 2010-2011 École Polytechnique
 * @version 1.2, 2011/09/28
 */
public interface Layer {

  /**
   * Specifies to this layer an other layer to which the incoming packets must
   * be forwarded to be processed at above layers. Then, this layer will invoke
   * the {@link #receive receive} method of the specified {@code above} layer to
   * pass it the packets going upwards.
   * 
   * @param above
   *          the {@code Layer} whose {@link #receive receive} method will be
   *          called for passing the packets going upwards
   */
  public void deliverTo(Layer above);

  /**
   * Handles an incoming packet at this layer. This method is invoked from the
   * layer below, to pass it any packet going upward. This method should not
   * block and will return as soon as possible. The implementation of this rule
   * may result in silently ignoring the given packet.
   * 
   * @param packet
   *          the {@code Packet} that must be handled by this layer
   * @param from
   *          the (lower) {@code Layer} that fetched the packet and is
   *          forwarding it to this method
   */
  public void receive(Packet packet, Layer from);

  /**
   * Sends a packet downwards through this layer. Depending on the layer
   * implementation, this method may block (because buffers are full) or not.
   * 
   * @param packet
   *          a {@code Packet} to be sent
   */
  public void send(Packet packet);

  /**
   * Closes this layer. This method should make the best effort to flush the
   * sending buffers and deliver all the enqueued messages at the corresponding
   * layer to destination.
   */
  public void close();
}

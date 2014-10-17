package common;

import packets.Packet;

/**
 * This class provides a partial implementation of the {@link Layer} interface
 * to simplify the definition of an applicative layer located at the top of the
 * stack. <br/>
 * Such a layer has no layer above and, therefore, the {@link #send send} and
 * {@link #deliverTo deliverTo} methods do not make sense. Thus both methods are
 * defined to throw an {@code UnsupportedOperationException}. <br/>
 * The {@link #close close} method is empty but will be overridden when needed. <br/>
 * A definition of the {@link #receive receive} method is missing and it must be
 * provided in any extending class.
 * 
 * @see Layer
 * 
 * @author Thomas Clausen
 * @author Philippe Chassignet
 * @author INF557, DIX, © 2010-2011 École Polytechnique
 * @version 1.2, 2011/09/28
 */

public abstract class TopLayer implements Layer {

  /**
   * Throws an {@code UnsupportedOperationException}, since a {@code TopLayer}
   * has no layer above.
   * 
   * @throws UnsupportedOperationException
   */
  public void deliverTo(Layer aboveLayer) {
    throw new UnsupportedOperationException("a TopLayer has no layer above");
  }

  /**
   * Throws an {@code UnsupportedOperationException}, since a {@code TopLayer}
   * doesn't support sending for any other layer.
   * 
   * @throws UnsupportedOperationException
   */
  public void send(Packet packet) {
    throw new UnsupportedOperationException(
        "a TopLayer doesn't support sending for any other layer");
  }

  /** Does nothing (to be overridden when needed). */
  public void close() { // empty
  }

}

package common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * This class implements facilities for reading the standard input stream
 * {@code System.in}, that is by default the keyboard.
 * 
 * @author Thomas Clausen
 * @author Florian Richoux
 * @author Vincent Siles
 * @author Philippe Chassignet
 * @author INF557, DIX, © 2010-2011 École Polytechnique
 * @version 1.2, 2011/09/28
 */
public class Keyboard {
  private static InputStreamReader reader = new InputStreamReader(System.in);
  private static BufferedReader input = new BufferedReader(reader);

  private Keyboard() {
    // Don't instantiate this class.
  }

  /**
   * Waits for a line (a sequence of characters ended by the a line-termination
   * character) read from the input stream and returns this line as a
   * {@code String}. The line-termination characters are not part of the
   * returned {@code String}.
   * 
   * @return a {@code String} containing the line, not including the
   *         line-termination characters, or {@code null} if the end of the
   *         input stream has been reached
   */
  public static String readString() {
    String readLine = null;
    try {
      readLine = input.readLine();
    } catch (IOException e) {
      System.err.println(e);
      System.exit(0); // kill program
    }
    return readLine;
  }
}

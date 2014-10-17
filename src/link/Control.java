package link;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.StringTokenizer;

import packets.Packet;

import common.Keyboard;

/**
 * This program reads commands from the keyboard and sends them to the
 * <em>forwarding agent</em> that listens on port <tt>controlPort</tt> of
 * <tt>serverHost</tt>. Usage :
 * <tt>java java tools/Control serverHost controlPort</tt><br/>
 * <p>
 * Note that the semantic of commands is defined in the
 * <em>forwarding agent</em> and may vary, depending on the implementation.
 * 
 * @see Matrix
 * @see HubsPool
 * @author Philippe Chassignet
 * @author INF557, DIX, © 2010 ƒcole Polytechnique
 */
public class Control {

  // for sending commands
  private static TargetedLink control;
  // the tokenizer for the current command line
  private static StringTokenizer st;
  // for receiving results
  private static ServerSocket resultSocket;

  private static String forgePacket(String source, String destination,
      String type, String... extraFields) {
    return Packet.encode(Packet.pack(source, destination, type, ""
        + resultSocket.getLocalPort(), extraFields), 0);
  }

  private static String forgeContent(String source, String destination) {
    return Packet.encode(Packet.pack(source, destination, null, null), 0);
  }

  private static enum Command {
    HELP {
      @Override
      void doIt() {
        for (Command c : values())
          c.printHelp(); // local command
        System.out.print("> ");
      }

      @Override
      void printHelp() {
        System.out.println("\thelp : print this help");
      }
    },
    EXIT {
      @Override
      void doIt() {
        System.exit(0);
      }

      @Override
      void printHelp() {
        System.out.println("\texit : exit the Control program");
      }
    },
    NODES {
      @Override
      @SuppressWarnings("synthetic-access")
      void doIt() {
        // String msg = MessageFormat.packet("control", "server", "nodes",
        // resultSocket.getLocalPort());
        String msg = forgePacket("control", "server", "nodes");
        // Packet msg = new
        // RawPacket("control:server:nodes:"+resultSocket.getLocalPort());
        control.send(msg);
      }

      @Override
      void printHelp() {
        System.out.println("\tnodes : print the known node interfaces");
      }
    },
    LINKS {
      @Override
      @SuppressWarnings("synthetic-access")
      void doIt() {
        // String msg = MessageFormat.packet("control", "server", "links",
        // resultSocket.getLocalPort());
        String msg = forgePacket("control", "server", "links");
        control.send(msg);
      }

      @Override
      void printHelp() {
        System.out.println("\tlinks : print the current forwarding tables");
      }
    },
    ADD {
      @Override
      @SuppressWarnings("synthetic-access")
      void doIt() {
        if (st.countTokens() >= 2) {
          String source = st.nextToken();
          String destination = st.nextToken();
          // String msg = MessageFormat.packet("control", "server", "add",
          // resultSocket.getLocalPort(), node, neighborhood);
          String msg = forgePacket("control", "server", "add",
              forgeContent(source, destination));
          control.send(msg);
        }
      }

      @SuppressWarnings("synthetic-access")
      @Override
      void printHelp() {
        String msg = forgePacket("control", "server", "help_add", "");
        control.send(msg);
      }
    },
    DEL {
      @Override
      @SuppressWarnings("synthetic-access")
      void doIt() {
        if (st.countTokens() >= 2) {
          String source = st.nextToken();
          String destination = st.nextToken();
          // String msg = MessageFormat.packet("control", "server", "del",
          // resultSocket.getLocalPort(), node, neighborhood);
          String msg = forgePacket("control", "server", "del",
              forgeContent(source, destination));
          // resultSocket.getLocalPort(), node, neighborhood);
          control.send(msg);
        }
      }

      @Override
      @SuppressWarnings("synthetic-access")
      void printHelp() {
        String msg = forgePacket("control", "server", "help_del", "");
        control.send(msg);
      }
    },
    ;
    abstract void doIt();

    abstract void printHelp();
  }

  private static void handleCommand(String command) {
    st = new StringTokenizer(command, " ");
    if (st.hasMoreTokens()) {
      String cmd = st.nextToken().toUpperCase();
      try {
        Command.valueOf(cmd).doIt();
      } catch (IllegalArgumentException e) {
        System.err.println("unknown command " + cmd);
      }
    } else
      System.out.print("> ");
  }

  private static void startResultThread() {
    new Thread() {
      @SuppressWarnings("synthetic-access")
      @Override
      public void run() {
        // we are the server
        while (true) {
          InputStream results = null;
          // wait for a client connection
          try {
            results = resultSocket.accept().getInputStream();
          } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
          }
          BufferedReader reader = new BufferedReader(new InputStreamReader(
              results));
          // connection is open and readable
          System.out.println();
          try {
            String line = reader.readLine();
            // readLine will return null when connection is closed
            while (line != null) {
              System.out.println(line);
              line = reader.readLine();
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
          System.out.print("> ");
        }
      }
    }.start();
  }

  public static void main(String[] args) {
    if (args.length < 2) {
      System.out.println("Syntax : java tools/Control serverHost controlPort");
      System.exit(0);
    }
    // for sending commands
    control = new TargetedLink(args[0], Integer.parseInt(args[1]), "control");
    control.setSuccessRate(100);
    control.setMaxDelay(0);
    try {
      control.bind(new DatagramSocket());
    } catch (SocketException e) {
      System.err.println(e);
      System.exit(0);
    }
    // for receiving results
    try {
      resultSocket = new ServerSocket();
      resultSocket.bind(null);
    } catch (IOException e1) {
      e1.printStackTrace();
      System.exit(0);
    }
    startResultThread();
    System.out.println("enter \"help\" for a list of the commands");
    System.out.print("> ");
    while (true) {
      String s = Keyboard.readString();
      if (s != null)
        handleCommand(s);
    }
  }
}

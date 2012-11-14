package cn.kk.autovpn;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamRedirector extends Thread {
  private final InputStream  from;

  private final OutputStream to;

  public StreamRedirector(InputStream from, OutputStream to) {
    super();
    this.from = from;
    this.to = to;
  }

  @Override
  public void run() {
    int b;
    try {
      while (-1 != (b = this.from.read())) {
        // System.out.println(new String(buffer, 0, len));
        if ((this.to != null) && (this.to instanceof ByteArrayOutputStream)) {
          if (ConnectHelper.DEBUG) {
            System.out.print((char) b);
          }
          this.to.write(b);
          this.to.flush();

          ByteArrayOutputStream baos = (ByteArrayOutputStream) this.to;
          final byte[] data = baos.toByteArray();
          ConnectHelper.lastOutput = new String(data, "UTF-8");
          if (ConnectHelper.lastOutput.contains("Username:")) {
            ConnectHelper.setStatus(Status.Username);
          }
          if (ConnectHelper.lastOutput.contains("Password:")) {
            ConnectHelper.setStatus(Status.Password);
          } else if (ConnectHelper.lastOutput.contains("state: Connected")) {
            ConnectHelper.setStatus(Status.Connected);
          } else if (ConnectHelper.lastOutput.contains("state: Disconnected")) {
            ConnectHelper.setStatus(Status.Disconnected);
          }
          if (b == '\n') {
            baos.reset();
          }
        } else {
          if (ConnectHelper.DEBUG) {
            System.err.print((char) b);
          }
          this.to.write(b);
          this.to.flush();
        }
      }
    } catch (IOException e) {
      System.err.println(e.toString());
    }
  }
}

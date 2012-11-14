package cn.kk.autovpn;

/*  Copyright (c) 2010 Xiaoyun Zhu
 * 
 *  Permission is hereby granted, free of charge, to any person obtaining a copy  
 *  of this software and associated documentation files (the "Software"), to deal  
 *  in the Software without restriction, including without limitation the rights  
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell  
 *  copies of the Software, and to permit persons to whom the Software is  
 *  furnished to do so, subject to the following conditions:
 *  
 *  The above copyright notice and this permission notice shall be included in  
 *  all copies or substantial portions of the Software.
 *  
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR  
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,  
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE  
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER  
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,  
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN  
 *  THE SOFTWARE.  
 */
import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;

import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileSystemView;

public final class ConnectHelper {
  private static final String[] PATHS;

  private static final String   OS_NAME = System.getProperty("os.name").toLowerCase();

  public static final Runtime   RT      = Runtime.getRuntime();

  static String                 lastOutput;

  static Status                 status;

  private static Robot          rb;
  static {
    try {
      ConnectHelper.rb = new Robot();
    } catch (AWTException e) {
      e.printStackTrace();
    }
  }

  public static boolean         DEBUG;

  static {
    String path = System.getenv("PATH");
    if (path == null) {
      path = System.getenv("path");
    }
    if (path == null) {
      PATHS = new String[0];
    } else {
      PATHS = path.split(File.pathSeparator);
      int i = 0;
      for (String p : ConnectHelper.PATHS) {
        ConnectHelper.PATHS[i++] = p.trim() + File.separator;
      }
    }
  }

  public static File findExecutable(final String executable, final String folder) {
    File result = null;
    if (null != (result = ConnectHelper.getExecutable(executable, folder))) {
      return result;
    }
    for (String p : ConnectHelper.PATHS) {
      if (null != (result = ConnectHelper.getExecutable(executable, p + folder))) {
        return result;
      }
    }
    final String pfDirX86 = System.getenv("ProgramFiles(x86)");
    final String pfDir = System.getenv("ProgramFiles");
    final String pfDirW = System.getenv("ProgramW6432");
    if ((pfDirX86 != null) && (result != (result = ConnectHelper.getExecutable(executable, pfDirX86 + folder)))) {
      return result;
    }
    if ((pfDir != null) && (result != (result = ConnectHelper.getExecutable(executable, pfDir + folder)))) {
      return result;
    }
    if ((pfDirW != null) && (result != (result = ConnectHelper.getExecutable(executable, pfDirW + folder)))) {
      return result;
    }
    if (result != (result = ConnectHelper.getExecutable(executable, System.getenv("SystemDrive") + folder))) {
      return result;
    }
    for (File f : File.listRoots()) {
      final String root = f.getAbsolutePath();
      String path;
      if (pfDirX86 != null) {
        path = root.charAt(0) + pfDirX86.substring(1);
        if (null != (result = ConnectHelper.getExecutable(executable, path + File.separator + folder))) {
          return result;
        }
      }
      if (pfDir != null) {
        path = root.charAt(0) + pfDir.substring(1);
        if (null != (result = ConnectHelper.getExecutable(executable, path + File.separator + folder))) {
          return result;
        }
      }
      if (pfDirW != null) {
        path = root.charAt(0) + pfDirW.substring(1);
        if (null != (result = ConnectHelper.getExecutable(executable, path + File.separator + folder))) {
          return result;
        }
      }
      path = root + "Program Files";
      if (null != (result = ConnectHelper.getExecutable(executable, path + File.separator + folder))) {
        return result;
      }
      path = root + "Programme";
      if (null != (result = ConnectHelper.getExecutable(executable, path + File.separator + folder))) {
        return result;
      }
      if (null != (result = ConnectHelper.getExecutable(executable, root + File.separator + folder))) {
        return result;
      }
    }
    if ((result == null) && (folder.length() != 0)) {
      return ConnectHelper.findExecutable(executable, "");
    } else {
      return result;
    }
  }

  private static final File getExecutable(final String executable, final String path) {
    if (path == null) {
      return null;
    }
    File file = new File(path, executable);
    if (file.isFile()) {
      return file.getAbsoluteFile();
    } else if ((file = new File(path, executable + ".exe")).isFile()) {
      return file.getAbsoluteFile();
    } else if ((file = new File(path, executable + ".cmd")).isFile()) {
      return file.getAbsoluteFile();
    } else if ((file = new File(path, executable + ".bat")).isFile()) {
      return file.getAbsoluteFile();
    }
    return null;
  }

  public static boolean isWindows() {
    return ConnectHelper.OS_NAME.startsWith("windows");
  }

  public static boolean isMac() {
    return ConnectHelper.OS_NAME.startsWith("mac");
  }

  public static boolean isLinux() {
    return ConnectHelper.OS_NAME.startsWith("linux");
  }

  static Process proc;

  public static boolean destroy() {
    if (ConnectHelper.proc != null) {
      ConnectHelper.proc.destroy();
      return true;
    } else {
      return false;
    }
  }

  public static final int execAndWaitCommand(final String cmd, final InputStream in) {
    if (ConnectHelper.DEBUG) {
      System.out.println(cmd);
    }

    ConnectHelper.setStatus(Status.Preparing);
    if (in != null) {
      new Thread() {
        @Override
        public void run() {
          try {
            ConnectHelper.proc = ConnectHelper.RT.exec(cmd);
            new StreamRedirector(ConnectHelper.proc.getErrorStream(), new ByteArrayOutputStream()).start();
            new StreamRedirector(ConnectHelper.proc.getInputStream(), new ByteArrayOutputStream()).start();
            new StreamRedirector(in, ConnectHelper.proc.getOutputStream()).start();
            ConnectHelper.proc.waitFor();
          } catch (Exception e) {
            System.err.println(e.toString());
          }
        }
      }.start();
      return 0;
    } else {
      try {
        ConnectHelper.proc = ConnectHelper.RT.exec(cmd);
        new StreamRedirector(ConnectHelper.proc.getErrorStream(), new ByteArrayOutputStream()).start();
        new StreamRedirector(ConnectHelper.proc.getInputStream(), new ByteArrayOutputStream()).start();
        return ConnectHelper.proc.waitFor();
      } catch (Exception e) {
        System.err.println(e.toString());
        return -1;
      }
    }
  }

  public static final int execAndWaitCommand(final String cmd) {
    return ConnectHelper.execAndWaitCommand(cmd, null);
  }

  public static String getLastOutput() {
    return ConnectHelper.lastOutput;
  }

  public static boolean isConnected() {
    return ConnectHelper.status == Status.Connected;
  }

  static final FileSystemView FILE_SYSTEM = FileSystemView.getFileSystemView();

  public final static boolean isEmptyOrNotExists(final String file) {
    final File f = new File(file);
    return !f.isFile() || (f.length() == 0);
  }

  public static boolean isEmptyOrNull(final String text) {
    return (text == null) || text.isEmpty();
  }

  public final static boolean isNotEmptyOrNull(final String text) {
    return (text != null) && (text.length() > 0);
  }

  public static final FileInputStream findResourceAsStream(final String resource) throws IllegalArgumentException, IOException {
    final File file = ConnectHelper.findResource(resource);
    if (null != file) {
      return new FileInputStream(file);
    } else {
      return null;
    }
  }

  /**
   * <pre>
   * Find resource in possible directories:
   * 1. find in the running directory
   * 2. find in the user directory
   * 3. find in the user document directory
   * 4. find on the user desktop
   * 5. get from root of the running directory
   * 6. load from class path and system path
   * 7. find in all root directories e.g. C:, D:
   * 8. find in temporary directory
   * </pre>
   * 
   * @param resource
   * @return
   * @throws IllegalArgumentException
   * @throws IOException
   */
  public static final File findResource(final String resource) throws IllegalArgumentException {
    File resFile = null;
    if (new File(resource).isFile()) {
      // in run directory
      resFile = new File(resource);
    }
    if (resFile == null) {
      // in user directory
      final String dir = System.getProperty("user.home");
      if (!ConnectHelper.isEmptyOrNull(dir)) {
        if (new File(dir, resource).isFile()) {
          resFile = new File(dir, resource);
        }
      }
    }
    if (resFile == null) {
      // in user document directory
      final File dir = ConnectHelper.FILE_SYSTEM.getDefaultDirectory();
      if (dir != null) {
        if (new File(dir, resource).isFile()) {
          resFile = new File(dir, resource);
        }
      }
    }
    if (resFile == null) {
      // in user desktop directory
      final File dir = ConnectHelper.FILE_SYSTEM.getHomeDirectory();
      if (dir != null) {
        if (new File(dir, resource).isFile()) {
          resFile = new File(dir, resource);
        }
      }
    }
    if (resFile == null) {
      // get from root of run directory
      final File dir = new File("/");
      if (dir.isDirectory()) {
        if (new File(dir, resource).isFile()) {
          resFile = new File(dir, resource);
        }
      }
    }
    if (resFile == null) {
      // get from class path (root)
      final URL resUrl = ConnectHelper.class.getResource("/" + resource);
      if (resUrl != null) {
        try {
          // resFile = File.createTempFile(resource, null);
          resFile = new File(ConnectHelper.FILE_SYSTEM.getDefaultDirectory(), resource);
          ConnectHelper.writeToFile(ConnectHelper.class.getResourceAsStream("/" + resource), resFile);
        } catch (final IOException e) {
          System.err.println("从JAR导出'" + resource + "'时出错：" + e.toString());
        }
        if ((resFile != null) && !resFile.isFile()) {
          resFile = null;
        }
      }
    }
    if (resFile == null) {
      // find in root directories, e.g. c:\, d:\, e:\, x:\
      final File[] dirs = File.listRoots();
      for (final File dir : dirs) {
        if (dir.isDirectory()) {
          if (new File(dir, resource).isFile()) {
            resFile = new File(dir, resource);
          }
        }
      }
    }
    if (resFile == null) {
      // in temp directory
      final String dir = System.getProperty("java.io.tmpdir");
      if (!ConnectHelper.isEmptyOrNull(dir)) {
        if (new File(dir, resource).isFile()) {
          resFile = new File(dir, resource);
        }
      }
    }
    if (resFile != null) {
      if (ConnectHelper.DEBUG) {
        System.out.println("找到：" + resFile.getAbsolutePath());
      }
    } else {
      System.err.println("没有找到：" + resource);
    }
    return resFile;
  }

  public static final void write(final InputStream in, final OutputStream out) throws IOException {
    int len;
    while ((len = in.read(ConnectHelper.IO_BB.array())) > 0) {
      out.write(ConnectHelper.IO_BB.array(), 0, len);
    }
  }

  public static final void writeToFile(final InputStream in, final File file) throws IOException {
    final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
    try {
      int len;
      while ((len = in.read(ConnectHelper.IO_BB.array())) > 0) {
        out.write(ConnectHelper.IO_BB.array(), 0, len);
      }
    } finally {
      out.close();
    }
  }

  private static final ByteBuffer IO_BB = ByteBuffer.wrap(new byte[1024 * 8]);

  public static boolean contains(byte[] buffer, int start, int end, byte val) {
    for (int i = start; i <= end; i++) {
      if (buffer[i] == val) {
        return true;
      }
    }
    return false;
  }

  public static Status getStatus() {
    return ConnectHelper.status;
  }

  public static void writeConsoleInput(String str) {
    for (int i = 0; i < str.length(); i++) {
      // cleanup
      ConnectHelper.rb.keyPress(KeyEvent.VK_SHIFT);
      ConnectHelper.rb.keyRelease(KeyEvent.VK_SHIFT);

      char c = str.charAt(i);
      if (Character.isUpperCase(c)) {
        ConnectHelper.rb.keyPress(KeyEvent.VK_SHIFT);
        ConnectHelper.type(ConnectHelper.getKeyCode(c));
        ConnectHelper.rb.keyRelease(KeyEvent.VK_SHIFT);
      } else {
        ConnectHelper.type(ConnectHelper.getKeyCode(c));
      }
    }
  }

  private static int getKeyCode(int c) {
    if ((c >= 'a') && (c <= 'z')) {
      return c - ('a' - 'A');
    }
    return c;
  }

  private static void type(int c) {
    try {
      ConnectHelper.rb.keyPress(c);
      ConnectHelper.rb.keyRelease(c);
    } catch (IllegalArgumentException e) {
      System.err.println("不支持字符：" + (char) c);
    }
  }

  public static Status getStatus(File statusFile) throws FileNotFoundException, IOException {
    Status s = Status.Disconnected;
    BufferedReader in = new BufferedReader(new FileReader(statusFile));
    try {
      String l = null;
      while (null != (l = in.readLine())) {
        if (l.contains("Username:")) {
          s = Status.Username;
        }
        if (l.contains("Password:")) {
          s = Status.Password;
        } else if (l.contains("state: Connected")) {
          s = Status.Connected;
        } else if (l.contains("state: Disconnected")) {
          s = Status.Disconnected;
        }
      }
    } finally {
      in.close();
    }
    ConnectHelper.setStatus(s);
    return s;
  }

  static void setStatus(Status s) {
    ConnectHelper.status = s;
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        Main.updateCtrl();
      }
    });
  }

  public static boolean isValidCfgValue(final String val) {
    return ConnectHelper.isNotEmptyOrNull(val) && val.replaceAll("[0-9a-zA-Z\\-\\.\\,]", "").isEmpty();
  }
}

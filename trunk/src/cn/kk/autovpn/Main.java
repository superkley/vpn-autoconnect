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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;

public class Main {
  private static final int    CHECK_CYCLE_SECONDS = 10;
  private final static String TITLE_CMD           = "VPN Autoconnect 1.0 for Cisco AnyConnect";
  private final static String VPNCLI_CMD          = "vpncli.exe";

  private static final String TITLE               = "VPN Autoconnect 1.0";

  Properties                  cfg                 = new Properties();

  private static final JLabel itmCtrl             = new JLabel();

  public Main(String cfgFile) throws IOException {
    InputStream in = new FileInputStream(cfgFile);
    try {
      this.cfg.load(in);
    } finally {
      in.close();
    }
    if (!(this.testCfg("host") && this.testCfg("user") && this.testCfg("password"))) {
      ConfigDialog dlgConfig = new ConfigDialog(cfgFile);
      dlgConfig.open();
      InputStream in2 = new FileInputStream(cfgFile);
      try {
        this.cfg.load(in2);
      } finally {
        in2.close();
      }
    }
  }

  private static void showWindow() throws IOException {
    JFrame frm = new JFrame(Main.TITLE);
    frm.setResizable(false);
    BufferedImage icon = ImageIO.read(Main.class.getResource("/icon.png"));
    frm.setIconImage(icon);
    JPanel pnl = new JPanel(new GridLayout(1, 1));
    pnl.setBackground(Color.WHITE);
    Main.itmCtrl.setFont(Main.itmCtrl.getFont().deriveFont(28f).deriveFont(Font.BOLD));
    Main.itmCtrl.setHorizontalAlignment(SwingConstants.CENTER);
    Main.itmCtrl.setVerticalAlignment(SwingConstants.CENTER);
    pnl.add(Main.itmCtrl);
    frm.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    });
    pnl.setPreferredSize(new Dimension(100, 80));
    frm.setContentPane(pnl);
    frm.pack();
    Main.updateCtrl();
    frm.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    frm.setVisible(true);
  }

  static void updateCtrl() {
    String text;
    if (Status.Connected == ConnectHelper.getStatus()) {
      text = "已连接";
      Main.itmCtrl.setForeground(Color.GREEN.darker());
    } else {
      text = "未连接";
      Main.itmCtrl.setForeground(Color.RED.darker());
    }
    Main.itmCtrl.setText(text);
  }

  private boolean testCfg(String key) {
    final String val = this.cfg.getProperty(key);
    if (ConnectHelper.isEmptyOrNull(this.cfg.getProperty(key))) {
      JOptionPane.showMessageDialog(null, "没有找到autovpn.cfg里'" + key + "'的设置！", Main.TITLE, JOptionPane.ERROR_MESSAGE);
      return false;
    }
    if (!ConnectHelper.isValidCfgValue(val)) {
      JOptionPane.showMessageDialog(null, key + "目前只能支持大小写字母，数字及以下字符：\n- . ,", Main.TITLE, JOptionPane.ERROR_MESSAGE);
      return false;
    }
    return true;

  }

  void start() {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      // silent
    }

    // RuntimeHelper.SILENT = true;
    ConnectHelper.execAndWaitCommand("taskkill /f /im \"" + Main.VPNCLI_CMD + "\" /t");
    ConnectHelper.execAndWaitCommand("taskkill /f /im \"vpnui.exe\" /t");

    String host = this.cfg.getProperty("host");
    String user = this.cfg.getProperty("user");
    String password = this.cfg.getProperty("password");
    try {
      // System.out.println(PPS_DATA_DIR);
      File cmd = ConnectHelper.findExecutable(Main.VPNCLI_CMD, "Cisco\\Cisco AnyConnect Secure Mobility Client");
      if (cmd != null) {
        // String script = null;
        // File scriptFile = new File(ConnectHelper.FILE_SYSTEM.getDefaultDirectory(), "vpn-autoconnect.Psc1");
        // try (ByteArrayOutputStream out = new ByteArrayOutputStream();
        // BufferedInputStream in = new BufferedInputStream(ConnectHelper.findResourceAsStream("vpn-autoconnect.Ptpl"));) {
        // ConnectHelper.write(in, out);
        // script = new String(out.toByteArray(), "UTF-8");
        // script = script.replace("${host}", host).replace("${user}", user).replace("${pass}", password).replace("${vpncli}", cmd.getAbsolutePath());
        // ConnectHelper.writeToFile(new ByteArrayInputStream(script.getBytes("UTF-8")), scriptFile);
        // }
        // if (ConnectHelper.isEmptyOrNull(script)) {
        // JOptionPane.showMessageDialog(null, "没有找到vpn-autoconnect.Ptpl！", Main.TITLE, JOptionPane.ERROR_MESSAGE);
        // System.exit(-3);
        // }
        while (true) {
          while (true) {
            ConnectHelper.execAndWaitCommand("\"" + cmd.getAbsolutePath() + "\" status");
            if (ConnectHelper.isConnected()) {
              TimeUnit.SECONDS.sleep(Main.CHECK_CYCLE_SECONDS);
            } else {
              TimeUnit.SECONDS.sleep(1);
              break;
            }
          }
          // System.out.println("运行：" + scriptFile.getAbsolutePath());
          // ConnectHelper.execAndWaitCommand("powershell -PSConsoleFile \"" + scriptFile.getAbsolutePath() + "\"");
          // TimeUnit.SECONDS.sleep(30);
          final File statusFile = File.createTempFile("vpn-autoconnect", ".out");
          if (ConnectHelper.DEBUG) {
            System.out.println("status: " + statusFile.getAbsolutePath());
          }
          DynamicInputStream in = new DynamicInputStream();
          try {
            String cmdConnect = cmd.getName() + " connect " + host;
            System.out.println("vpncli连接命令：" + cmdConnect);
            String cmdStart = "cmd /C start \"" + Main.TITLE_CMD + "\" /D \"" + cmd.getParentFile().getAbsolutePath() + "\" /MIN cmd /C \"" + cmdConnect // +
                                                                                                                                                         // "\"";
                + " 2>&1 >^\"" + statusFile.getAbsolutePath() + "^\"\"";
            if (ConnectHelper.DEBUG) {
              System.out.println("start: " + cmdStart);
            }
            ConnectHelper.execAndWaitCommand(cmdStart, in);
            // this.sendKeyInputs(cmdConnect);
            // ConnectHelper.execAndWaitCommand(cmdConnect, in);
            for (int i = 0; i < 10; i++) {
              TimeUnit.SECONDS.sleep(1);
              if (Status.Username == ConnectHelper.getStatus(statusFile)) {
                break;
              }
            }
            this.sendKeyInputs(user);
            this.sendKeyInputs("\n\n");
            for (int i = 0; i < 10; i++) {
              TimeUnit.SECONDS.sleep(1);
              if (Status.Password == ConnectHelper.getStatus(statusFile)) {
                break;
              }
            }
            this.sendKeyInputs(password);
            this.sendKeyInputs("\n\n");
            // for (int i = 0; i < password.length(); i++) {
            // in.setData(String.valueOf(password.charAt(i)).getBytes("UTF-8"));
            // TimeUnit.SECONDS.sleep(1);
            // }
            // in.setData("\n".getBytes("UTF-8"));
            for (int i = 0; i < 10; i++) {
              TimeUnit.SECONDS.sleep(1);
              if (Status.Connected == ConnectHelper.getStatus(statusFile)) {
                System.out.println("VPN连接成功");
                break;
              }
            }
            ConnectHelper.destroy();
          } catch (Exception e) {
            e.printStackTrace();
          } finally {
            if (!statusFile.delete()) {
              statusFile.deleteOnExit();
            }
            in.close();
          }
        }
      } else {
        JOptionPane.showMessageDialog(null, "没有找到Cisco AnyConnect VPNCLI程序！", Main.TITLE, JOptionPane.ERROR_MESSAGE);
        System.exit(-5);
      }
    } catch (Exception e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(null, "用户没有足够的权限（请使用高级用户运行本软件！）：" + e.toString(), Main.TITLE, JOptionPane.ERROR_MESSAGE);
      System.exit(-1);
    }
  }

  private void sendKeyInputs(String str) throws InterruptedException {
    HWND hWndBackup = User32.INSTANCE.GetForegroundWindow();
    HWND hWnd = User32.INSTANCE.FindWindow(null, Main.TITLE_CMD);
    if (hWnd != null) {
      User32.INSTANCE.SetForegroundWindow(hWnd);
      ConnectHelper.writeConsoleInput(str);
      if (hWndBackup != null) {
        User32.INSTANCE.SetForegroundWindow(hWndBackup);
      }
    }
  }

  public static void main(String[] args) throws IOException {
    File cfg = ConnectHelper.findResource("autovpn.cfg");
    if (cfg == null) {
      Properties props = new Properties();
      props.put("host", "vpn.uni-mannheim.de");
      props.put("user", "");
      props.put("password", "");
      final String cfgFile = System.getProperty("user.home") + "/autovpn.cfg";
      FileOutputStream out = new FileOutputStream(cfgFile);
      try {
        props.store(out, null);
      } finally {
        out.close();
      }
      ConfigDialog dlgConfig = new ConfigDialog(cfgFile);
      dlgConfig.open();
      cfg = ConnectHelper.findResource("autovpn.cfg");
    }
    if (cfg == null) {
      JOptionPane.showMessageDialog(null, "请先更改autovpn.cfg里的设置：\nhost=<HOST>\nuser=<用户名>\npassword=<密码>", Main.TITLE, JOptionPane.ERROR_MESSAGE);
      System.exit(-9);
    } else {
      System.out.println("读出设置：" + cfg.getAbsolutePath());
      final Main main = new Main(cfg.getAbsolutePath());
      new Thread() {
        @Override
        public void run() {
          main.start();
        }
      }.start();
      Main.showWindow();
    }
  }
}

package cn.kk.autovpn;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

public class ConfigDialog extends JDialog {
  private static final long serialVersionUID = 3737965937447743057L;
  private final JTextField  itmHost          = new JTextField(16);
  private final JTextField  itmUser          = new JTextField(16);
  private final JTextField  itmPassword      = new JTextField(16);
  private final Properties  cfg              = new Properties();
  private final String      cfgFile;

  public ConfigDialog(String cfgFile) throws FileNotFoundException, IOException {
    this.setTitle("VPN设置");
    this.setLocationRelativeTo(null);
    this.cfgFile = cfgFile;
    JPanel pnl = new JPanel(new GridLayout(3, 2, 6, 6));
    pnl.setBorder(new EmptyBorder(6, 6, 6, 6));
    pnl.add(new JLabel("服务器网址"));
    pnl.add(this.itmHost);
    pnl.add(new JLabel("用户名"));
    pnl.add(this.itmUser);
    pnl.add(new JLabel("密码"));
    pnl.add(this.itmPassword);
    pnl.setPreferredSize(new Dimension(300, 100));
    this.setContentPane(pnl);
    this.setResizable(false);
    this.setModalityType(ModalityType.APPLICATION_MODAL);
    this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        if (ConfigDialog.this.check()) {
          ConfigDialog.this.setVisible(false);
          ConfigDialog.this.dispose();
        }
      }
    });
    InputStream in = new FileInputStream(cfgFile);
    try {
      this.cfg.load(in);
    } finally {
      in.close();
    }
    this.itmHost.setText(this.cfg.getProperty("host"));
    this.itmUser.setText(this.cfg.getProperty("user"));
    this.itmPassword.setText(this.cfg.getProperty("password"));

    KeyAdapter checker = new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        ConfigDialog.this.check();
      }
    };

    this.itmHost.addKeyListener(checker);
    this.itmUser.addKeyListener(checker);
    this.itmPassword.addKeyListener(checker);

    this.check();
    this.pack();
  }

  boolean check() {
    final String host = this.itmHost.getText();
    final String user = this.itmUser.getText();
    final String password = this.itmPassword.getText();

    boolean validUser = ConnectHelper.isValidCfgValue(user);
    boolean validHost = ConnectHelper.isValidCfgValue(host);
    boolean validPassword = ConnectHelper.isValidCfgValue(password);

    if (!validUser) {
      this.itmUser.setBackground(Color.RED);
    } else {
      this.itmUser.setBackground(Color.WHITE);
    }

    if (!validHost) {
      this.itmHost.setBackground(Color.RED);
    } else {
      this.itmHost.setBackground(Color.WHITE);
    }

    if (!validPassword) {
      this.itmPassword.setBackground(Color.RED);
    } else {
      this.itmPassword.setBackground(Color.WHITE);
    }
    return validHost & validUser & validPassword;
  }

  public void open() throws IOException {
    this.setVisible(true);
    this.cfg.setProperty("host", this.itmHost.getText());
    this.cfg.setProperty("user", this.itmUser.getText());
    this.cfg.setProperty("password", this.itmPassword.getText());
    FileOutputStream out = new FileOutputStream(this.cfgFile);
    try {
      this.cfg.store(out, null);
    } finally {
      out.close();
    }
  }
}

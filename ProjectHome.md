# VPN AutoConnect for Cisco AnyConnect Secure Mobility Clients #

Cisco AnyWhere VPN自动连接程序。输入服务器，用户名，密码后，这个小程序在后台自动连接VPN。


如需要启动电脑时自动运行，请复制此程序到您的自动启动项里（"所有程序 > 启动"）。


## 下载 / Download ##

Windows: http://vpn-autoconnect.googlecode.com/files/vpn-autoconnect-1.0.0.exe

Java (Jar): http://vpn-autoconnect.googlecode.com/files/vpn-autoconnect-1.0.0.jar



![http://vpn-autoconnect.googlecode.com/files/screen_status.png](http://vpn-autoconnect.googlecode.com/files/screen_status.png)

![http://vpn-autoconnect.googlecode.com/files/screen_config.png](http://vpn-autoconnect.googlecode.com/files/screen_config.png)

注：

1. Cisco AnyWhere VPN Client必须已安装在电脑上。

2. 只支持Windows系统

3. 程序每十秒检查一次。再输入密码时会开出新的窗口。（登陆大概需要3秒钟时间）

4. 程序启动后会重启vpnui状态显示

5. 状态显示可以通过命令行的--status开启。程序会随状态窗口关闭而关闭

因为Java不支持vpncli里用到的WriteConsoleInput。所以本程序运用JNA以及Java Robot来输入密码。因为Robot的限制，密码只能为字母或者数字。


This program connects / reconnects to the cisco vpnagent server automatically.


Requirements:

**JVM 1.6 and above**

**Windows**

**Cisco AnyWhere VPN Client installed**


This software stores your vpn configuration (Password in clear text).


Please copy a shortcut of this program to your autorun folder to run it on every computer startup.
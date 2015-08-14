package com.github.xsavikx.android.screencast.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.android.ddmlib.IDevice;
import com.github.xsavikx.android.screencast.api.AndroidDevice;
import com.github.xsavikx.android.screencast.api.command.executor.CommandExecutor;
import com.github.xsavikx.android.screencast.api.command.factory.AdbInputCommandFactory;
import com.github.xsavikx.android.screencast.api.injector.ConstEvtKey;
import com.github.xsavikx.android.screencast.api.injector.Injector;
import com.github.xsavikx.android.screencast.api.injector.KeyCodeConverter;
import com.github.xsavikx.android.screencast.api.injector.ScreenCaptureThread.ScreenCaptureListener;
import com.github.xsavikx.android.screencast.ui.explorer.JFrameExplorer;

public class JFrameMain extends JFrame {

  private static final long serialVersionUID = -2085909236767692371L;
  private JPanelScreen jp = new JPanelScreen();
  private JToolBar jtb = new JToolBar();
  private JToolBar jtbHardkeys = new JToolBar();
  private JToggleButton jtbRecord = new JToggleButton("Record");

  private JButton jbOpenUrl = new JButton("Open Url");
  JScrollPane jsp;
  private JButton jbExplorer = new JButton("Explore");
  private JButton jbRestartClient = new JButton("Restart client");
  private JButton jbKbHome = new JButton("Home");
  private JButton jbKbMenu = new JButton("Menu");
  private JButton jbKbBack = new JButton("Back");
  private JButton jbKbSearch = new JButton("Search");

  private JButton jbKbPhoneOn = new JButton("Call");

  private JButton jbKbPhoneOff = new JButton("End call");
  private IDevice device;
  private CommandExecutor commandExecutor;
  private Injector injector;
  private Dimension oldImageDimension = null;

  public JFrameMain(IDevice device) {
    this.device = device;
    initialize();
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {

      @Override
      public boolean dispatchKeyEvent(KeyEvent e) {
        if (!JFrameMain.this.isActive())
          return false;
        if (commandExecutor == null)
          return false;
        if (e.getID() == KeyEvent.KEY_TYPED) {
          int code = KeyCodeConverter.getKeyCode(e);
          commandExecutor.execute(AdbInputCommandFactory.getKeyCommand(code));
        }
        return false;
      }
    });
  }

  public void initialize() {
    jtb.setFocusable(false);
    jbExplorer.setFocusable(false);
    jtbRecord.setFocusable(false);
    jbOpenUrl.setFocusable(false);
    jbKbHome.setFocusable(false);
    jbKbMenu.setFocusable(false);
    jbKbBack.setFocusable(false);
    jbKbSearch.setFocusable(false);
    jbKbPhoneOn.setFocusable(false);
    jbKbPhoneOff.setFocusable(false);
    jbRestartClient.setFocusable(false);

    jbKbHome.addActionListener(new KbActionListener(ConstEvtKey.KEYCODE_HOME));
    jbKbMenu.addActionListener(new KbActionListener(ConstEvtKey.KEYCODE_MENU));
    jbKbBack.addActionListener(new KbActionListener(ConstEvtKey.KEYCODE_BACK));
    jbKbSearch.addActionListener(new KbActionListener(ConstEvtKey.KEYCODE_SEARCH));
    jbKbPhoneOn.addActionListener(new KbActionListener(ConstEvtKey.KEYCODE_CALL));
    jbKbPhoneOff.addActionListener(new KbActionListener(ConstEvtKey.KEYCODE_ENDCALL));

    jtbHardkeys.add(jbKbHome);
    jtbHardkeys.add(jbKbMenu);
    jtbHardkeys.add(jbKbBack);
    jtbHardkeys.add(jbKbSearch);
    jtbHardkeys.add(jbKbPhoneOn);
    jtbHardkeys.add(jbKbPhoneOff);

    // setIconImage(Toolkit.getDefaultToolkit().getImage(
    // getClass().getResource("icon.png")));
    setDefaultCloseOperation(3);
    setLayout(new BorderLayout());
    add(jtb, BorderLayout.NORTH);
    add(jtbHardkeys, BorderLayout.SOUTH);
    jsp = new JScrollPane(jp);
    add(jsp, BorderLayout.CENTER);
    jsp.setPreferredSize(new Dimension(100, 100));
    pack();
    setLocationRelativeTo(null);

    MouseAdapter ma = new InjectMouseAdapter();

    jp.addMouseMotionListener(ma);
    jp.addMouseListener(ma);
    jp.addMouseWheelListener(ma);

    jtbRecord.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent arg0) {
        if (jtbRecord.isSelected()) {
          startRecording();
        } else {
          stopRecording();
        }
      }

    });
    jtb.add(jtbRecord);

    jbExplorer.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent arg0) {
        JFrameExplorer jf = new JFrameExplorer(device);
        jf.setIconImage(getIconImage());
        jf.setVisible(true);
      }
    });
    jtb.add(jbExplorer);

    jtb.add(jbRestartClient);

    jbOpenUrl.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        JDialogUrl jdUrl = new JDialogUrl();
        jdUrl.setVisible(true);
        if (!jdUrl.result)
          return;
        String url = jdUrl.jtfUrl.getText();
        new AndroidDevice(device).openUrl(url);
      }
    });
    jtb.add(jbOpenUrl);

  }

  public void setInjector(Injector injector) {
    this.injector = injector;
    injector.screencapture.setListener(new ScreenCaptureListener() {

      @Override
      public void handleNewImage(Dimension size, BufferedImage image, boolean landscape) {
        if (oldImageDimension == null || !size.equals(oldImageDimension)) {
          jsp.setPreferredSize(size);
          JFrameMain.this.pack();
          oldImageDimension = size;
        }
        jp.handleNewImage(size, image, landscape);
      }
    });
  }

  private void startRecording() {
    JFileChooser jFileChooser = new JFileChooser();
    FileNameExtensionFilter filter = new FileNameExtensionFilter("Video file", "mov");
    jFileChooser.setFileFilter(filter);
    int returnVal = jFileChooser.showSaveDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      injector.screencapture.startRecording(jFileChooser.getSelectedFile());
    }
  }

  private void stopRecording() {
    injector.screencapture.stopRecording();
  }

  public CommandExecutor getCommandExecutor() {
    return commandExecutor;
  }

  public void setCommandExecutor(CommandExecutor commandExecutor) {
    this.commandExecutor = commandExecutor;
  }

  class InjectMouseAdapter extends MouseAdapter {
    private int dragFromX = -1;

    private int dragFromY = -1;
    private long timeFromPress = -1;
    private final static long ONE_SECOND = 1000L;

    @Override
    public void mouseClicked(MouseEvent e) {
      if (injector != null && e.getButton() == MouseEvent.BUTTON3) {
        injector.screencapture.toogleOrientation();
        e.consume();
        return;
      }
      if (commandExecutor == null)
        return;
      Point p2 = jp.getRawPoint(e.getPoint());
      commandExecutor.execute(AdbInputCommandFactory.getTapCommand(p2.x, p2.y));
    }

    @Override
    public void mouseDragged(MouseEvent e) {
      if (dragFromX == -1 && dragFromY == -1) {
        Point p2 = jp.getRawPoint(e.getPoint());
        dragFromX = p2.x;
        dragFromY = p2.y;
        timeFromPress = System.currentTimeMillis();
      }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      if (commandExecutor == null) {
        return;
      }
      if (timeFromPress >= ONE_SECOND) {
        Point p2 = jp.getRawPoint(e.getPoint());
        commandExecutor.execute(AdbInputCommandFactory.getSwipeCommand(dragFromX, dragFromY, p2.x, p2.y));
        dragFromX = -1;
        dragFromY = -1;
        timeFromPress = -1;
      }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent arg0) {
      // if (JFrameMain.this.injector == null)
      // return;
      // JFrameMain.this.injector.injectTrackball(arg0.getWheelRotation() < 0 ?
      // -1f : 1f);
    }
  }

  public class KbActionListener implements ActionListener {

    int key;

    public KbActionListener(int key) {
      this.key = key;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (commandExecutor == null)
        return;
      commandExecutor.execute(AdbInputCommandFactory.getKeyCommand(key));
    }

  }
}

package org.secta.spiral;

import org.secta.spiral.common.LogUtils;
import org.secta.spiral.common.ProgressListener;
import org.secta.spiral.common.RemoteFile;
import org.secta.spiral.patcher.Patch;
import org.secta.spiral.patcher.Patcher;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Launcher {
  private static final Logger LOG = LogUtils.getLog();

  private static final String STEAM_APP_ID = "99900";
  private static final String DEFAULT_GAME_ROOT_PATH = "./";
  private static final String ABOUT_URL = "https://vk.com/@spiralkachan-rusifikator-spiral-knights";

  private static Patcher patcher;

  static class JPanelWithBackground extends JPanel {
    private Image backgroundImage;

    public JPanelWithBackground(final Image image) {
      backgroundImage = image;
    }

    public void paintComponent(final Graphics g) {
      super.paintComponent(g);
      // Draw the background image.
      g.drawImage(backgroundImage, 0, 0, this);
    }
  }

  private static void createAndShowGUI() {
    JFrame.setDefaultLookAndFeelDecorated(true);

    //Create and set up the window.
    JFrame frame = new JFrame("Spiral launcher");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setUndecorated(true);
    frame.setResizable(false);

    JPanel controls = new JPanel(null);
    controls.setOpaque(false);

    try {
      final BufferedImage image = ImageIO.read(Launcher.class.getClassLoader().getResourceAsStream("launcher.jpg"));
      final Dimension d = new Dimension(image.getWidth(), image.getHeight());
      final JPanel root = new JPanelWithBackground(image);
      root.setLayout(new GridLayout(1, 1));
      controls.setPreferredSize(d);
      root.setSize(d);
      root.add(controls);
      frame.getContentPane().add(root);
      frame.setPreferredSize(d);
    } catch (IOException e) {
      e.printStackTrace();
    }

    final Font font = new Font("Arial", Font.BOLD, 12);

    JLabel versionLabel = new JLabel("Версия русификатора:");
    versionLabel.setSize(versionLabel.getPreferredSize().width + 50, versionLabel.getPreferredSize().height);
    versionLabel.setLocation(525, 58);
    versionLabel.setFont(font);
    versionLabel.setForeground(Color.white);

    JLabel versionVal = new JLabel("?");
    versionVal.setSize(versionLabel.getPreferredSize());
    versionVal.setLocation(716, 58);
    versionVal.setFont(font);
    versionVal.setForeground(Color.white);

    JProgressBar progressBar = new JProgressBar();
    progressBar.setMinimum(0);
    progressBar.setMaximum(100);
    // progressBar.setIndeterminate(true);
    progressBar.setSize(210, 10);
    progressBar.setLocation(525, 85);

    JButton runButton = new JButton("Запустить игру");
    runButton.setEnabled(false);
    runButton.setLocation(630, 370);
    runButton.setSize(runButton.getPreferredSize());

    runButton.addActionListener(actionEvent -> {
      try {
        new SteamGameExecutor().startGameById(STEAM_APP_ID);
      } catch (final Exception e) {
        LOG.log(Level.SEVERE, "Failed to start steamapp " + STEAM_APP_ID, e);
      }
    });

    final JTextArea textArea = new JTextArea();
    textArea.setEditable(false);
    textArea.setOpaque(false);
    textArea.setFont(new Font("Arial", Font.ITALIC, 12));
    textArea.setForeground(Color.white);

    final JScrollPane scrollPane = new JScrollPane(textArea);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.getViewport().setOpaque(false);
    scrollPane.setOpaque(false);
    scrollPane.setBorder(null);

    scrollPane.setLocation(340, 118);
    scrollPane.setSize(400, 225);

    runPatcher(progress -> {
      int val = Math.round(progress * 100);
      if (val > progressBar.getValue()) {
        progressBar.setValue(val);
      }
    }, textArea, versionVal, runButton);

    JButton aboutButton = new JButton("?");
    aboutButton.setLocation(570, 370);
    aboutButton.setSize(50, runButton.getHeight());
    aboutButton.addActionListener(actionEvent -> {
      final Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
      if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
        try {
          desktop.browse(URI.create(ABOUT_URL));
        } catch (final Exception e) {
          LOG.log(Level.WARNING, e.getMessage(), e);
        }
      }
    });

    controls.add(scrollPane);
    controls.add(versionLabel);
    controls.add(versionVal);
    controls.add(progressBar);
    controls.add(runButton);
    controls.add(aboutButton);

    //Display the window.
    frame.pack();

    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    frame.setLocation(dim.width / 2 - frame.getSize().width / 2, dim.height / 2 - frame.getSize().height / 2);

    frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "Exit");
    frame.getRootPane().getActionMap().put("Exit", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        System.exit(0);
      }
    });

    frame.setVisible(true);
  }

  private static void runPatcher(final ProgressListener listener, final JTextArea text, final JLabel ver, final JButton button) {
    SwingWorker worker = new SwingWorker() {
      @Override
      protected Object doInBackground() {
        try {
          final GithubClient client = new GithubClient();
          final Collection<RemoteFile> files = client.getFiles("Spiralka-Chan", "spiralization");
          listener.update(0.1f);
          final Patch patch = new Patch(files);
          if (patch.getPatchNotes() != null) {
            text.setText(patch.getPatchNotes());
            ver.setText(patch.getVersion());
          }
          if (patcher.patchAll(patch, listener)) {
            button.setEnabled(true);
          }
        } catch (final Exception e) {
          LOG.log(Level.SEVERE, e.getMessage(), e);
          fatalError("Ошибка", "Случилось нечто непредвиденное:\n" + e.getMessage());
        }
        return null;
      }

      ;
    };
    worker.execute();
  }

  private static void fatalError(final String title, final String message) {
    JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    System.exit(1);
  }

  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      //UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (UnsupportedLookAndFeelException e) {
      e.printStackTrace();
    }

    final String rootPath = args.length > 0 ? args[0] : DEFAULT_GAME_ROOT_PATH;

    ResourceBundle rb = ResourceBundle.getBundle("story");


    LOG.info("Started with " + rootPath);

    patcher = new Patcher(rootPath);
    if (!patcher.isConfigured()) {
      LOG.severe(rootPath + " does not seem to be a game root path");
      fatalError(
          "Ошибка",
          "Директория " + rootPath + " не похожа на корневую директорию игры.\n" +
              "Проверьте, что исполняемый файл находится в директории с игрой."
      );
    }

    javax.swing.SwingUtilities.invokeLater(Launcher::createAndShowGUI);
  }
}

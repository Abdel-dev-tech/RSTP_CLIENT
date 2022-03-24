/*
 * Author: Jonatan Schroeder
 * Updated: March 2022
 *
 * This code may not be used without written consent of the authors.
 */

package ca.yorku.rtsp.client.ui;

import javax.swing.*;

public class VideoControlToolbar extends JToolBar {

    private MainWindow main;
    private JButton openButton, playButton, pauseButton;
    private JButton closeButton;
    private JButton disconnectButton;

    public VideoControlToolbar(MainWindow mainWindow) {

        this.main = mainWindow;

        setFloatable(false);

        openButton = new JButton("Open");
        openButton.addActionListener(e -> {
            String videoName = JOptionPane.showInputDialog("Video file:");
            if (videoName != null) main.getSession().open(videoName);
        });
        this.add(openButton);

        this.addSeparator();

        playButton = new JButton("Play");
        playButton.addActionListener(e -> main.getSession().play());
        this.add(playButton);

        pauseButton = new JButton("Pause");
        pauseButton.addActionListener(e -> main.getSession().pause());
        this.add(pauseButton);

        this.addSeparator();

        closeButton = new JButton("Close");
        closeButton.addActionListener(e -> main.getSession().close());
        this.add(closeButton);

        this.addSeparator();

        disconnectButton = new JButton("Disconnect");
        disconnectButton.addActionListener(e -> main.disconnect(true));
        this.add(disconnectButton);
    }
}

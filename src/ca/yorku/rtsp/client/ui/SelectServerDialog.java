/*
 * Author: Jonatan Schroeder
 * Updated: March 2022
 *
 * This code may not be used without written consent of the authors.
 */

package ca.yorku.rtsp.client.ui;

import ca.yorku.rtsp.client.exception.RTSPException;
import ca.yorku.rtsp.client.model.Session;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.text.DecimalFormat;

public class SelectServerDialog extends JFrame implements ActionListener {

    private MainWindow mainWindow;

    private JLabel jidServerLabel, portLabel;
    private JTextField jidServerField;
    private JFormattedTextField portField;
    private JButton connectButton;
    private JButton cancelButton;

    private GenericFormPanel formPanel;

    public SelectServerDialog(MainWindow mainWindow) {

        super("Select RTSP server");
        // super(mainWindow, Dialog.ModalityType.DOCUMENT_MODAL);

        this.mainWindow = mainWindow;

        jidServerLabel = new JLabel("RTSP Server: ");
        jidServerField = new JTextField(10);
        jidServerLabel.setLabelFor(jidServerField);

        portLabel = new JLabel("Port: ");
        portField = new JFormattedTextField(new DecimalFormat("####0"));
        portField.setValue(554);
        portLabel.setLabelFor(portField);

        connectButton = new JButton("Connect");
        connectButton.addActionListener(this);

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> System.exit(0));

        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        formPanel = new GenericFormPanel();

        formPanel.addLineOfFields(jidServerLabel, jidServerField);
        formPanel.addLineOfFields(portLabel, portField);

        formPanel.addButton(connectButton);
        formPanel.addButton(cancelButton);

        this.add(formPanel);

        this.getRootPane().setDefaultButton(connectButton);

        this.setSize(600, 200);
        this.setLocation((mainWindow.getWidth() - this.getWidth()) / 2, (mainWindow.getHeight() - this.getHeight()) / 2);

        loadInfo();
    }

    @Override
    public void actionPerformed(ActionEvent event) {

        try {
            saveInfo();

            Session session = new Session(jidServerField.getText(), Integer.parseInt(portField.getText()));
            mainWindow.setSession(session);
            this.setVisible(false);
            mainWindow.setVisible(true);

        } catch (RTSPException e) {
            mainWindow.exceptionThrown(e);
        }
    }

    private File savedInfoFile = new File(System.getProperty("user.home"), ".rtp.client.txt");

    private void saveInfo() {
        try {

            PrintStream writer = new PrintStream(new FileOutputStream(savedInfoFile));
            writer.println(jidServerField.getText());
            writer.println(portField.getText());
            writer.close();

        } catch (Exception e) {
            // Ignore
            e.printStackTrace();
        }
    }

    private void loadInfo() {

        if (!savedInfoFile.exists()) return;

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(savedInfoFile)));
            jidServerField.setText(reader.readLine());
            portField.setText(reader.readLine());
            reader.close();
        } catch (Exception e) {
            // Ignore
            e.printStackTrace();
        }
    }
}

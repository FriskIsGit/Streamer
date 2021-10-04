
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.SwingConstants;

import java.awt.Font;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.IOException;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;


class Streamer{
    private Socket clientSocket1;
    private Socket clientSocket2;
    private ServerSocket serverSocket1;
    private final JFrame frame;
    private final int TIMEOUT_MS;
    private final JTextField status;
    protected Streamer(){
        frame = new JFrame("Streamer");
        status = new JTextField("STATUS");
        TIMEOUT_MS = 3000;
    }

    public static void main(String[] args){
        Streamer streamerApp = new Streamer();
        streamerApp.setup();
    }

    private void setup(){
        runFrame();
        JButton connectToButton = new JButton("Connect to desktop");
        JButton createServerButton = new JButton("Create server");
        JButton backButton = new JButton("BACK");
        JButton submitButton = new JButton("Submit");

        JTextField ipField = new JTextField("10.0.0.");
        JTextField portField = new JTextField("port");
        JCheckBox ipBindCheck = new JCheckBox("bind to ip");


        connectToButton.setFont(new Font("Candara", Font.BOLD, 18));
        connectToButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent con){
                connectToButton.setVisible(false);
                createServerButton.setVisible(false);
                backButton.setVisible(true);
                ipField.setVisible(true);
                portField.setVisible(true);
                submitButton.setVisible(true);
            }
        });
        frame.add(connectToButton);

        createServerButton.setFont(new Font("Candara", Font.BOLD, 20));
        createServerButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent create){
                connectToButton.setVisible(false);
                createServerButton.setVisible(false);
                backButton.setVisible(true);
                ipBindCheck.setVisible(true);
                portField.setVisible(true);
                submitButton.setVisible(true);
                if (ipBindCheck.isSelected()){
                    ipField.setVisible(true);
                }
            }
        });
        frame.add(createServerButton);

        backButton.setFont(new Font("Candara", Font.ITALIC, 25));
        backButton.setFocusable(false);
        backButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent back){
                backButton.setVisible(false);
                connectToButton.setVisible(true);
                createServerButton.setVisible(true);
                ipBindCheck.setVisible(false);
                portField.setVisible(false);
                submitButton.setVisible(false);
                ipField.setVisible(false);
            }
        });
        backButton.setVisible(false);
        frame.add(backButton);

        ipBindCheck.setBackground(Color.gray);
        ipBindCheck.setFont(new Font("Candara", Font.BOLD, 20));
        ipBindCheck.setHorizontalAlignment(SwingConstants.CENTER);
        ipBindCheck.setForeground(Color.black);
        ipBindCheck.setVisible(false);
        ipBindCheck.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent checked){
                ipField.setVisible(ipBindCheck.isSelected());
            }
        });
        frame.add(ipBindCheck);

        ipField.setBackground(Color.BLACK);
        ipField.setCaretColor(Color.white);
        ipField.setForeground(Color.white);
        ipField.setFont(new Font("Arial Rounded MT Bold", Font.BOLD, 20));
        ipField.setVisible(false);
        frame.add(ipField);

        portField.setBackground(Color.BLACK);
        portField.setCaretColor(Color.white);
        portField.setForeground(Color.white);
        portField.setFont(new Font("Arial Rounded MT Bold", Font.BOLD, 20));
        portField.setVisible(false);
        frame.add(portField);


        submitButton.setFont(new Font("Candara", Font.BOLD, 20));
        submitButton.setFocusable(false);
        submitButton.setVisible(false);
        //connection related stuff
        submitButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent submit){
                //server creation
                String strIP = ipField.getText();
                String portStr = portField.getText();
                if (portStr.length() > 5 || !portStr.matches("[0-9]+")){
                    status.setText("Invalid port");
                    return;
                }
                if (ipBindCheck.isVisible()){
                    try{
                        if (!ipBindCheck.isSelected()){
                            serverSocket1 = new ServerSocket(Integer.parseInt(portStr));
                        }else{
                            serverSocket1 = new ServerSocket(Integer.parseInt(portStr), 2, InetAddress.getByName(strIP));
                        }
                    }catch (IOException ioException){
                        status.setText("Failed to create server");
                        return;
                    }
                    status.setText("Server Port: " + serverSocket1.getLocalPort());
                    AwaitConnection waiter1 = new AwaitConnection(serverSocket1, status, frame, backButton, submitButton, ipField, portField, ipBindCheck);
                    waiter1.execute();
                }
                //client connects
                else{
                    try{
                        int port = Integer.parseInt(portStr);
                        //always creating a new socket to prevent closed socket exceptions
                        clientSocket1 = new Socket();
                        clientSocket1.connect(new InetSocketAddress(strIP, port), TIMEOUT_MS);
                        status.setText("Connected to server");
                        establishConnectionOnSecondPort(strIP);
                        hideUnnecessaryComponents();
                        ConnectionFrame connection = new ConnectionFrame(frame, clientSocket1, clientSocket2);
                        connection.run();

                    }catch (IOException err){
                        err.printStackTrace();
                        status.setText("Failed to connect");
                    }
                }
            }

            private void establishConnectionOnSecondPort(String IP) throws IOException{
                DataInputStream portStream = new DataInputStream(clientSocket1.getInputStream());
                int secondPort = portStream.readInt();
                clientSocket2 = new Socket();
                clientSocket2.connect(new InetSocketAddress(IP, secondPort), TIMEOUT_MS);
            }

            private void hideUnnecessaryComponents(){
                backButton.setVisible(false);
                submitButton.setVisible(false);
                ipField.setVisible(false);
                portField.setVisible(false);
            }
        });
        frame.add(submitButton);

        status.setFont(new Font("Caladea", Font.BOLD, 20));
        status.setForeground(Color.white);
        status.setEditable(false);
        status.setHorizontalAlignment(SwingConstants.CENTER);
        status.setBackground(Color.gray);
        status.setCaretColor(Color.white);
        frame.add(status);

        frame.revalidate();
    }

    private void runFrame(){
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 300);
        frame.setLocation(50, 50);
        frame.getContentPane().setBackground(Color.gray);
        frame.setLayout(new GridLayout(0, 2));
        frame.setVisible(true);
    }
}

package app;

import javax.swing.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

class AwaitConnection extends SwingWorker<Object, Object>{
    private Socket acceptedSocket;
    private final ServerSocket serverSocket;
    private final JTextField status;
    private final JFrame rootFrame;
    private final JButton b1;
    private final JButton b2;
    private final JTextField f1;
    private final JTextField f2;
    private final JCheckBox box;

    protected AwaitConnection(ServerSocket ss, JTextField status, JFrame rootFrame, JButton b1, JButton b2, JTextField f1, JTextField f2, JCheckBox box){
        this.serverSocket = ss;
        this.status = status;
        this.rootFrame = rootFrame;
        this.b1 = b1;
        this.b2 = b2;
        this.f1 = f1;
        this.f2 = f2;
        this.box = box;
    }

    @Override
    protected Socket doInBackground() throws IOException{
        System.out.println("Awaiting client..");
        acceptedSocket = serverSocket.accept();
        return null;
    }

    @Override
    protected void done(){
        status.setText("Connection Established");
        Connection connection = new Connection(this.rootFrame, this.acceptedSocket);
        connection.run();
        hideComponents();
    }

    private void hideComponents(){
        this.b1.setVisible(false);
        this.b2.setVisible(false);
        this.f1.setVisible(false);
        this.f2.setVisible(false);
        this.box.setVisible(false);
    }
}





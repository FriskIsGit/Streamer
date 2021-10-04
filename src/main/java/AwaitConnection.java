import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

class AwaitConnection extends SwingWorker<Object, Object>{
    private Socket acceptedSocket1;
    private Socket acceptedSocket2;
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
        acceptedSocket1 = serverSocket.accept();
        ServerSocket tempServerSocket = new ServerSocket(0);
        System.out.println("Writing to stream");
        DataOutputStream portOutputStream = new DataOutputStream(acceptedSocket1.getOutputStream());
        portOutputStream.writeInt(tempServerSocket.getLocalPort());
        System.out.println("Waiting on second port");
        acceptedSocket2 = tempServerSocket.accept();
        return null;
    }

    @Override
    protected void done(){
        status.setText("Connection Established");
        ConnectionFrame connection = new ConnectionFrame(this.rootFrame,this.acceptedSocket1,this.acceptedSocket2);
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





package app;

import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;

/**
 * JPEG image files begin with FF D8 and end with FF D9
 * start 255,216; end 255,217
 * actual -1 -40    -1 -39 jpg
 * 77 90 -112   115 111 110
 */
class Connection{

    public BufferedImage receivedImage = null;
    private final Display display;
    private final JFrame rootFrame;
    private DataOutputStream dos;
    private DataInputStream dis;
    private Robot bot;
    private final Rectangle screenRectangle;
    volatile private boolean streaming = false;
    volatile private boolean watching = false;
    final static private int CHUNK_SIZE_BYTES = 30720;
    final static private int MB = 1048576;
    //private final LinkedList<Integer> times = new LinkedList<>();
    final private Color RED = new Color(255, 74, 0);
    final private Color GREEN = new Color(64, 180, 0);

    protected Connection(JFrame rootFrame, Socket socket) {
        this.rootFrame = rootFrame;
        try{
            this.dos = new DataOutputStream(socket.getOutputStream());
            this.dis = new DataInputStream(socket.getInputStream());
        }catch (IOException ioException){
            System.err.println("Error thrown at streams");
        }
        display = new Display(1200, 800, "--STREAM--"){
            @Override
            public void draw(Graphics2D gfx){
                if(receivedImage != null){
                    gfx.drawImage(receivedImage,0,0, null);
                }
                else{
                    System.out.println("Received image is null");
                }
            }
        };

        //screenDimensions = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension screenDimensions = new Dimension(1920,1080);
        screenRectangle = new Rectangle(screenDimensions);
        try{
            bot = new Robot();
        }catch (AWTException roboExc){
            roboExc.printStackTrace();
        }
    }

    protected void run(){
        addChoicesToRoot();
        startThreads();
    }

    private void startThreads(){
        Thread streamingThread = new Thread(new Runnable(){
            @Override
            public void run(){
                System.out.println("Entered streaming loop");

                while(true){
                    sleep(100);
                    while (streaming){
                        BufferedImage screenImg = bot.createScreenCapture(screenRectangle);
                        try{
                            ImageIO.write(screenImg,"jpeg", dos);
                        }catch (IOException ioExc){
                            ioExc.printStackTrace();
                        }
                    }
                }
            }
        });

        Thread receivingThread = new Thread(new Runnable(){

            @Override
            public void run(){
                System.out.println("Receiving thread started");

                while (true){
                    sleep(100);
                    while (watching){
                        try{
                            int totalBytes = 0;
                            int currentRead;
                            byte[] bytes = new byte[MB];
                            do{
                                currentRead = dis.read(bytes, totalBytes, CHUNK_SIZE_BYTES);
                                totalBytes += currentRead;
                            }while (!(bytes[totalBytes-2]==-1 && bytes[totalBytes-1]==-39));

                            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes, 0, totalBytes);
                            receivedImage = ImageIO.read(inputStream);
                            if(receivedImage.getWidth() != display.getWidth() || receivedImage.getHeight() != display.getHeight()){
                                receivedImage = Scalr.resize(receivedImage, display.getWidth(), display.getHeight());
                            }
                        }catch (IOException | IndexOutOfBoundsException exc){}
                        display.renderFrame();
                    }
                }
            }
        });

        streamingThread.start();
        receivingThread.start();
    }

    private void addChoicesToRoot(){
        JButton watchStreamButton = new JButton("Watch Stream");
        JButton streamButton = new JButton("Stream Yourself");

        watchStreamButton.setMultiClickThreshhold(100);
        watchStreamButton.setBackground(Color.black);
        watchStreamButton.setForeground(Color.lightGray);
        watchStreamButton.setFocusable(false);
        watchStreamButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                if(watching){
                    display.frame.setVisible(false);
                    watchStreamButton.setForeground(RED);
                    watching = false;
                }
                else{
                    display.frame.setVisible(true);
                    watchStreamButton.setForeground(GREEN);
                    watching = true;
                }

            }
        });
        rootFrame.add(watchStreamButton);

        streamButton.setMultiClickThreshhold(50);
        streamButton.setBackground(Color.black);
        streamButton.setForeground(Color.lightGray);
        streamButton.setFocusable(false);
        streamButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                if(streaming) {
                    streamButton.setForeground(RED);
                    streaming = false;
                }
                else{
                    streamButton.setForeground(GREEN);
                    streaming = true;
                }
            }
        });
        rootFrame.add(streamButton);
    }

    private void sleep(int time){
        try{
            Thread.sleep(time);
        }catch (InterruptedException iExc){
            iExc.printStackTrace();
        }
    }
}

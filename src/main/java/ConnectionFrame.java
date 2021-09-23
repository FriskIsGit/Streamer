import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

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
class ConnectionFrame{

    private final JFrame streamFrame;
    private final JFrame rootFrame;
    private DataOutputStream dos;
    private DataInputStream dis;
    private Robot bot;
    private final Dimension screenDimensions;
    private final Rectangle screenRectangle;
    volatile private boolean streaming = false;
    volatile private boolean watching = false;
    final static private int CHUNK_SIZE_BYTES = 30000;
    final static private int MB = 1048576;
    //private final LinkedList<Integer> times = new LinkedList<>();
    final private Color RED;
    final private Color GREEN;

    protected ConnectionFrame(JFrame frame, Socket socket) {
        this.rootFrame = frame;
        this.RED = new Color(255, 74, 0);
        this.GREEN = new Color(64, 180, 0);
        try{
            this.dos = new DataOutputStream(socket.getOutputStream());
            this.dis = new DataInputStream(socket.getInputStream());
        }catch (IOException ioException){
            System.err.println("Error thrown at streams");
        }
        rootFrame.setLocation(0,10);
        streamFrame = new JFrame("--STREAM--");
        //screenDimensions = Toolkit.getDefaultToolkit().getScreenSize();
        screenDimensions = new Dimension(1920,1080);
        screenRectangle = new Rectangle(screenDimensions);
        try{
            bot = new Robot();
        }catch (AWTException roboExc){
            roboExc.printStackTrace();
        }
    }


    protected void run(){
        addChoicesToRoot();
        setup();
        startThreads();
    }

    private void startThreads(){
        Thread streamingThread = new Thread(new Runnable(){
            @Override
            public void run(){
                System.out.println("Entered streaming loop");
                ByteArrayOutputStream os = new ByteArrayOutputStream();

                JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(os);
                while(true){
                    sleep(200);
                    while (streaming){
                        BufferedImage img = bot.createScreenCapture(screenRectangle);
                        os.reset();
                        try{
                            //long st = System.currentTimeMillis();
                            encoder.encode(img);
                            //long en = System.currentTimeMillis();
                            //times.add((int) (en-st));
                            //ImageIO.write(img,"jpg", os);
                            byte [] jpgBytes = os.toByteArray();
                            final int JPG_SIZE = jpgBytes.length;
                            int totalBytes = 0;
                            while(true){
                                if(totalBytes + CHUNK_SIZE_BYTES > JPG_SIZE){
                                    dos.write(jpgBytes,totalBytes,JPG_SIZE-totalBytes);
                                    //dos.flush();
                                    break;
                                }
                                dos.write(jpgBytes,totalBytes,CHUNK_SIZE_BYTES);
                                totalBytes+=CHUNK_SIZE_BYTES;
                            }
                        }catch (IOException ioExc){
                            ioExc.printStackTrace();
                        }
                    }
                    /*int size = times.size();
                    if(size==0) continue;
                    System.out.println("Enumerating");
                    long sum=0;
                    for(int lap : times){
                        sum+=lap;
                    }
                    times.clear();
                    System.out.println("Average: " + (sum / size));*/
                }
            }
        });

        Thread receivingThread = new Thread(new Runnable(){
            private static final int PANE_SIDE_DIST = 8;
            private static final int PANE_TOP_DIST = 8;

            @Override
            public void run(){
                System.out.println("Entered receiving loop");
                Graphics frameGraphics = streamFrame.getGraphics();
                sleep(200);
                streamFrame.setVisible(false);
                while (true){
                    sleep(200);
                    while (watching){

                        try{
                            int totalBytes = 0;
                            int currentRead;
                            byte[] bytes = new byte[MB];
                            do{
                                currentRead = dis.read(bytes, totalBytes, CHUNK_SIZE_BYTES);
                                totalBytes+=currentRead;
                            }while (!(bytes[totalBytes-2]==-1 && bytes[totalBytes-1]==-39));
                            //long st = System.currentTimeMillis();
                            frameGraphics.drawImage(ImageIO.read(new ByteArrayInputStream(bytes, 0, totalBytes)), PANE_SIDE_DIST, PANE_TOP_DIST, null);
                            //long en = System.currentTimeMillis();
                            //System.out.println((en-st));
                        }catch (IOException  | IndexOutOfBoundsException exc){}
                    }
                }
            }
        });

        streamingThread.start();
        receivingThread.start();
    }

    private void addChoicesToRoot(){
        JButton watchStream = new JButton("Watch Stream");
        JButton streamButton = new JButton("Stream Yourself");

        watchStream.setMultiClickThreshhold(100);
        watchStream.setBackground(Color.black);
        watchStream.setForeground(Color.lightGray);
        watchStream.setFocusable(false);
        watchStream.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                if(watching){
                    streamFrame.setVisible(false);
                    Graphics graphics = streamFrame.getGraphics();
                    graphics.setColor(Color.BLACK);
                    graphics.fillRect(0,0,streamFrame.getWidth(),streamFrame.getHeight());
                    streamFrame.repaint();
                    watchStream.setForeground(RED);
                    watching = false;
                }
                else{
                    streamFrame.setVisible(true);
                    watchStream.setForeground(GREEN);
                    watching = true;
                }

            }
        });
        rootFrame.add(watchStream);

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

    private void setup(){
        rootFrame.setLocation(0,20);
        //streamFrame.setSize(screenDimensions);
        final int height = (int)screenDimensions.getHeight()/2;
        streamFrame.setSize((int)screenDimensions.getWidth()/2, height);
        streamFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        streamFrame.getContentPane().setBackground(Color.black);
        streamFrame.setVisible(true);
    }
    private void sleep(int time){
        try{
            Thread.sleep(time);
        }catch (InterruptedException iExc){
            iExc.printStackTrace();
        }
    }
}



// TODO: 2021-09-24 AffineTransform, FULL screen mode, HOST ARCH (BTW) BASED SCREEN SHARING NETWORK,
///////////////////////////////////////////////////////////////////////////
// JPEG image files begin with FF D8 and end with FF D9
//  start 255,216; end 255,217
//  actual -1 -40    -1 -39 jpg
///////////////////////////////////////////////////////////////////////////


import uk.ac.manchester.tornado.api.annotations.Parallel;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JFrame;
import javax.swing.JButton;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


class ConnectionFrame{

    private final JFrame streamFrame;
    private final JFrame rootFrame;
    private DataOutputStream dos1;
    private DataOutputStream dos2;
    private DataInputStream dis1;
    private DataInputStream dis2;
    private Robot bot;
    private final Dimension screenDimensions;
    private final Rectangle screenRectangle;
    volatile private boolean streaming = false;
    volatile private boolean watching = false;
    final static private int CHUNK_SIZE_BYTES = 30000;
    final static private int MB = 1_048_576;
    final static private double SCREEN_WIDTH = 1920;
    final static private double SCREEN_HEIGHT = 1080;
    //private final LinkedList<Integer> times = new LinkedList<>();
    final private Color RED;
    final private Color GREEN;
    final private Lock STREAMING_LOCK = new ReentrantLock();
    final private Lock WATCHING_LOCK = new ReentrantLock();

    protected ConnectionFrame(JFrame frame, Socket socket1, Socket socket2) {
        this.rootFrame = frame;
        this.RED = new Color(255, 74, 0);
        this.GREEN = new Color(64, 180, 0);
        try{
            this.dos1 = new DataOutputStream(socket1.getOutputStream());
            this.dis1 = new DataInputStream(socket1.getInputStream());
            this.dos2 = new DataOutputStream(socket2.getOutputStream());
            this.dis2 = new DataInputStream(socket2.getInputStream());
        }catch (IOException ioException){
            System.err.println("Error thrown at streams");
        }
        rootFrame.setLocation(0,10);
        streamFrame = new JFrame("--STREAM--");
        //screenDimensions = Toolkit.getDefaultToolkit().getScreenSize();
        screenDimensions = new Dimension((int)SCREEN_WIDTH,(int)SCREEN_HEIGHT);
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
    @SuppressWarnings("all")
    private void startThreads(){
        @Parallel
        Thread streamingThread1 = new Thread(new Runnable(){
            @Override
            public void run(){
                System.out.println("Entered streaming loop1");

                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                //encoder = JPEGCodec.createJPEGEncoder(os);
                ImageOutputStream ios = null;
                try{
                    ios = ImageIO.createImageOutputStream(baos);
                }catch (IOException ioException){
                    ioException.printStackTrace();
                }
                try{
                    bot = new Robot();
                }catch (AWTException e){
                    e.printStackTrace();
                }
                while(true){
                    sleep(200);
                    while (streaming){
                        @Parallel
                        BufferedImage bfrdImg = bot.createScreenCapture(screenRectangle);
                        @Parallel
                        RenderedImage image = (RenderedImage) bfrdImg;
                        try{
                            //long st = System.currentTimeMillis();
                            ImageIO.write(image,"jpg", ios);
                            //encoder.encode(image);
                            //long en = System.currentTimeMillis();
                            //list.add((int) (en-st));
                            byte [] jpgBytes = baos.toByteArray();
                            final int JPG_SIZE = jpgBytes.length;
                            int totalBytes = 0;
                            while(true){
                                if(totalBytes + CHUNK_SIZE_BYTES > JPG_SIZE){
                                    dos1.write(jpgBytes,totalBytes,JPG_SIZE-totalBytes);
                                    break;
                                }
                                dos1.write(jpgBytes,totalBytes,CHUNK_SIZE_BYTES);
                                totalBytes+=CHUNK_SIZE_BYTES;
                            }
                        }catch (IOException ioExc){
                            ioExc.printStackTrace();
                        }
                        baos.reset();
                    }

                }
            }
        });

        @Parallel
        Thread streamingThread2 = new Thread(new Runnable(){
            @Override
            public void run(){
                System.out.println("Entered streaming loop2");

                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                ImageOutputStream ios = null;
                try{
                    ios = ImageIO.createImageOutputStream(baos);
                }catch (IOException ioException){
                    ioException.printStackTrace();
                }
                try{
                    bot = new Robot();
                }catch (AWTException e){
                    e.printStackTrace();
                }
                while (true){
                    sleep(200);
                    while (streaming){
                        @Parallel
                        BufferedImage bfrdImg;

                        STREAMING_LOCK.lock();
                        try{
                            bfrdImg = bot.createScreenCapture(screenRectangle);
                        }finally{
                            STREAMING_LOCK.unlock();
                        }
                        @Parallel
                        RenderedImage image = (RenderedImage) bfrdImg;
                        try{
                            ImageIO.write(image, "jpg", ios);
                            byte[] jpgBytes = baos.toByteArray();
                            final int JPG_SIZE = jpgBytes.length;
                            int totalBytes = 0;
                            while (true){
                                if (totalBytes + CHUNK_SIZE_BYTES > JPG_SIZE){
                                    dos2.write(jpgBytes, totalBytes, JPG_SIZE - totalBytes);
                                    break;
                                }
                                dos2.write(jpgBytes, totalBytes, CHUNK_SIZE_BYTES);
                                totalBytes += CHUNK_SIZE_BYTES;
                            }
                            baos.reset();
                        }catch (IOException ioExc){
                            ioExc.printStackTrace();
                        }
                    }
                }
            }
        });
        @Parallel
        Thread receivingThread1 = new Thread(new Runnable(){
            private static final int PANE_SIDE_DIST = 8;
            private static final int PANE_TOP_DIST = 30;

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
                                currentRead = dis1.read(bytes, totalBytes, CHUNK_SIZE_BYTES);
                                totalBytes+=currentRead;
                            }while (!(bytes[totalBytes-2]==-1 && bytes[totalBytes-1]==-39));
                            @Parallel
                            Image receivedImg;
                            WATCHING_LOCK.lock();
                            try{
                                receivedImg = ImageIO.read(new ByteArrayInputStream(bytes, 0, totalBytes));
                            }finally{
                                WATCHING_LOCK.unlock();
                            }

                            if(receivedImg==null) continue;
                            try{

                                receivedImg = receivedImg.getScaledInstance(streamFrame.getWidth() - 16,streamFrame.getHeight() - 38, Image.SCALE_AREA_AVERAGING);
                                //final AffineTransform affineScale = AffineTransform.getScaleInstance((double)(streamFrame.getWidth() - 16) / SCREEN_WIDTH, (double)(streamFrame.getHeight() - 38)/ SCREEN_HEIGHT);
                                //final AffineTransformOp ato = new AffineTransformOp(affineScale, AffineTransformOp.TYPE_BICUBIC);
                                //BufferedImage scaledImg = new BufferedImage(streamFrame.getWidth(), streamFrame.getHeight(), BufferedImage.TYPE_INT_ARGB);
                                //frameGraphics.drawImage(ato.filter(receivedImg, scaledImg), PANE_SIDE_DIST, PANE_TOP_DIST, null);
                                frameGraphics.drawImage(receivedImg,PANE_SIDE_DIST,PANE_TOP_DIST,null);
                            }catch (Exception exc){
                                exc.printStackTrace();
                            }


                        }catch (IOException  | IndexOutOfBoundsException ignored){}
                    }
                }
            }
        });
        @Parallel
        Thread receivingThread2 = new Thread(new Runnable(){
            private static final int PANE_SIDE_DIST = 8;
            private static final int PANE_TOP_DIST = 30;

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
                                currentRead = dis2.read(bytes, totalBytes, CHUNK_SIZE_BYTES);
                                totalBytes += currentRead;
                            }while (!(bytes[totalBytes - 2] == -1 && bytes[totalBytes - 1] == -39));
                            Image receivedImg;

                            WATCHING_LOCK.lock();
                            try{
                                receivedImg = ImageIO.read(new ByteArrayInputStream(bytes, 0, totalBytes));
                            }finally{
                                WATCHING_LOCK.unlock();
                            }
                            if (receivedImg == null) continue;
                            try{
                                receivedImg = receivedImg.getScaledInstance(streamFrame.getWidth() - 16, streamFrame.getHeight() - 38, Image.SCALE_AREA_AVERAGING);
                                //final AffineTransform affineScale = AffineTransform.getScaleInstance((double)(streamFrame.getWidth() - 16) / SCREEN_WIDTH, (double)(streamFrame.getHeight() - 38)/ SCREEN_HEIGHT);
                                //final AffineTransformOp ato = new AffineTransformOp(affineScale, AffineTransformOp.TYPE_BICUBIC);
                                //BufferedImage scaledImg = new BufferedImage(streamFrame.getWidth(), streamFrame.getHeight(), BufferedImage.TYPE_INT_ARGB);
                                //frameGraphics.drawImage(ato.filter(receivedImg, scaledImg), PANE_SIDE_DIST, PANE_TOP_DIST, null);
                                frameGraphics.drawImage(receivedImg, PANE_SIDE_DIST, PANE_TOP_DIST, null);
                            }catch (Exception exc){
                                exc.printStackTrace();
                            }


                        }catch (IOException | IndexOutOfBoundsException ignored){
                        }
                    }
                }
            }
        });

        streamingThread1.start();
        receivingThread1.start();
        streamingThread2.start();
        receivingThread2.start();
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

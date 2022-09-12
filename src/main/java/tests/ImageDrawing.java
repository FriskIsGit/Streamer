package tests;

import app.Display;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;

public class ImageDrawing{
    static Robot bot;
    final static int WIDTH = 1400;
    final static int HEIGHT = 800;
    public static void main(String[] args){
        try{
            bot = new Robot();
        }catch (AWTException e){
            throw new RuntimeException(e);
        }
        final BufferedImage[] arr = new BufferedImage[1];
        arr[0] = getScreenshot();
        Display display = new Display(WIDTH, HEIGHT){
            @Override
            public void draw(Graphics2D gfx){
                gfx.drawImage(arr[0], 0, 0, null);
            }
        };
        System.out.println(Arrays.toString(ImageIO.getWriterFormatNames()));
        new Thread(new Runnable(){
            @Override
            public void run( ){
                int i = 0;
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                while(true){
                    i++;

                    arr[0] = getScreenshot();
                    os.reset();

                    try{
                        ImageIO.write(arr[0],"jpeg", os);
                        System.out.println(i +"#Written: " + os.size());
                    }catch (IOException e){
                        e.printStackTrace();
                    }

                    //System.out.println("Write: " + (en - st));
                    ByteArrayInputStream bais = new ByteArrayInputStream(os.toByteArray());

                    try{
                        arr[0] = ImageIO.read(bais);
                    }catch (IOException e){
                        throw new RuntimeException(e);
                    }

                    display.renderFrame();
                }
            }
        }).start();
        new Thread(new Runnable(){
            @Override
            public void run(){
                long last = 0;
                long current;
                while(true){
                    try{
                        Thread.sleep(1000);
                    }catch (InterruptedException e){
                        throw new RuntimeException(e);
                    }
                    current = display.getFrames();
                    System.out.println("FPS: " + (current-last));
                    last = current;

                }
            }
        }).start();


    }

    private static BufferedImage getScreenshot(){
        return bot.createScreenCapture(new Rectangle(1920, 1080));
    }
}


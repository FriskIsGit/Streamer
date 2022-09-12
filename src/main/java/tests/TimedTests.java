package tests;

import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;

public class TimedTests{
    @Test
    public void testImageIOWrite() throws RuntimeException, IOException{
        final int WRITES = 50;
        LinkedList<Long> times = new LinkedList<>();
        Robot bot;
        try{
            bot = new Robot();
        }catch (AWTException e){
            throw new RuntimeException(e);
        }
        Rectangle rect = new Rectangle(1920, 1080);
        BufferedImage screenshot = bot.createScreenCapture(rect);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (int i = 0; i < WRITES; i++){
            long st = System.currentTimeMillis();
            ImageIO.write(screenshot, "jpg", baos);
            long en = System.currentTimeMillis();
            times.add(en-st);
        }
        long sum = 0;
        for (Long t : times){
            sum+=t;
        }
        long average = sum/WRITES;
        System.out.println("Average out of " + WRITES + " jpg writes (in ms): " + average);
    }
    @Test
    public void testCaptureTime(){
        Robot bot;
        try{
            bot = new Robot();
        }catch (AWTException awtExc){
            throw new RuntimeException(awtExc);
        }
        Rectangle rect = new Rectangle(1920, 1080);
        long st = System.currentTimeMillis();
        bot.createScreenCapture(rect);
        long en = System.currentTimeMillis();
        System.out.println("Capture time (in ms): " + (en-st));
    }
}

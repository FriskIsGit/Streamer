package tests;

import java.io.ByteArrayOutputStream;

public class Projecting{
    final static private int MB = 1048576;
    public static void main(String[] args){
        long st = System.nanoTime();
        ByteArrayOutputStream os = new ByteArrayOutputStream(MB);
        long en = System.nanoTime();
        System.out.println("Diff: "  +(en-st));
    }
}

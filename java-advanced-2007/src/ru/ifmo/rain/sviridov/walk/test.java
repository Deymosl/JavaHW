package ru.ifmo.rain.sviridov.walk;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class test {
    public static void main(String[] args) throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("shit", true));
        bufferedWriter.write("ss" + "\n");
        bufferedWriter.close();
    }
}

package ru.ifmo.rain.sviridov.walk;


import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RecursiveWalk {
    private static final String USAGE = "java Walk <input-file> <output-file>";

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            if (args == null || args.length != 2) {
                System.err.println("Incorrect number of args");
            } else if (args[0] == null) {
                System.err.println("input-file must be non-null");
            } else if (args[1] == null) {
                System.err.println("output-file must be non-null");
            }
            System.err.println("USAGE " + USAGE);
            System.exit(1);
        }
        Path input = null, output = null;
        try {
            input = makePath(args[0], "Incorrect input-file's path");
            output = makePath(args[1], "Incorrect output-file's path");
            new RecursiveWalk().run(input, output);
        } catch (RecursiveWalkException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private static Path makePath(String path, String msg) throws RecursiveWalkException {
        try {
            return Paths.get(path);
        } catch (InvalidPathException e) {
            throw new RecursiveWalkException(msg);
        }
    }

    private void run(Path input, Path output) throws RecursiveWalkException {
        try (BufferedReader bufferedReader = Files.newBufferedReader(input, Charset.forName("UTF-8"))) {
            try (BufferedWriter bufferedWriter = Files.newBufferedWriter(output, Charset.forName("UTF-8"))) {
                RecursiveWalkTree recursiveWalkTree = new RecursiveWalkTree(bufferedWriter);

                String name;
                while (true) {
                    try {
                        if ((name = bufferedReader.readLine()) == null) break;
                        try {
                            Files.walkFileTree(Paths.get(name), recursiveWalkTree);
                        } catch (InvalidPathException | IOException e) {
                            System.err.println("Error while hashing a file: " +     e.getMessage());
                            bufferedWriter.write(String.format("%x", 0) + " " + name + "\n");
                        }
                    } catch (IOException e) {
                        throw new RecursiveWalkException("Can't read from input-file: " + e.getMessage());
                    }
                }

            } catch (IOException e) {
                throw new RecursiveWalkException(e.getMessage());
            }
        } catch (IOException e) {
            throw new RecursiveWalkException(e.getMessage());
        }
    }
}

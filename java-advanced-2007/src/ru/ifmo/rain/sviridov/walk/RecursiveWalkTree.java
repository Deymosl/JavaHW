package ru.ifmo.rain.sviridov.walk;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.TERMINATE;

public class RecursiveWalkTree extends SimpleFileVisitor<Path> {
    private final BufferedWriter bufferedWriter;
    private final int FNV_32_PRIME = 0x01000193;

    RecursiveWalkTree(BufferedWriter e) {
        bufferedWriter = e;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
        int result = 0x0;
        try (InputStream inputStream = Files.newInputStream(file)) {
            try {
                result = FVN1Hash(inputStream);
            } catch (IOException e) {
                System.err.println("Error while hashing a file " + file.toString() + " : " + e.getMessage());
                result = 0;
            }
        } catch (IOException | InvalidPathException e) {
            System.err.println("Can't access file " + file.toString() + ": " + e.getMessage());
            result = 0;
        }
        try {
            bufferedWriter.write(result + " " + file.toString() + "\n");
        } catch (IOException e) {
            System.err.println("Error writing to output-file: " + e.getMessage());
            return TERMINATE;
        }
        return CONTINUE;
    }


    int FVN1Hash(InputStream inputStream) throws IOException {
        int hval = 0x811c9dc5;
        int c = 0;
            while ((c = inputStream.read()) != -1) {
                hval *= FNV_32_PRIME;
                hval ^= c;
            }
        return hval;
    }

}

package ru.ifmo.rain.sviridov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler implements Crawler {
    private static final String USAGE = "USAGE: WebCrawler url [depth [downloads [extractors [perHost]]]]";

    private final Downloader downloader;
    private final int perHost;
    private final ExecutorService extractorsPool, downloadersPool;
    private final ConcurrentHashMap<String, SynchronizedHostDownloader> hosts = new ConcurrentHashMap<>();


    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.perHost = perHost;
        downloadersPool = Executors.newFixedThreadPool(downloaders);
        extractorsPool = Executors.newFixedThreadPool(extractors);
    }

    private class SynchronizedHostDownloader {

        private final Queue<Runnable> awaitingTasks;
        private int online;

        SynchronizedHostDownloader() {
            awaitingTasks = new ArrayDeque<>();
            online = 0;
        }

        synchronized void lock(boolean finish) {
            if (finish) {
                online--;
            }
            if (online < perHost) {
                executeNextTask();
            }
        }

        synchronized void addTask(Runnable task) {
             awaitingTasks.add(task);
            lock(false);
        }

        private synchronized void executeNextTask() {
            Runnable task = awaitingTasks.poll();
            if (task != null) {
                online++;
                downloadersPool.submit(() -> {
                    try {
                        task.run();
                    } finally {
                        lock(true);
                    }
                });
            }
        }
    }

    private class Controller {
        private final Phaser phaser = new Phaser(1);
        private final int depth;
        private final Set<String> extracted = new HashSet<>(), result = new HashSet<>();
        private final ConcurrentMap<String, IOException> acquiredExceptions = new ConcurrentHashMap<>();
        private final ConcurrentLinkedQueue<String> areWaiting = new ConcurrentLinkedQueue<>();
        private int count = 0;

        Controller(String url, int depth) {
            areWaiting.add(url);
            this.depth = depth;
            run();
        }

        private void run() {
            phaser.register();
            for (int cD = 0; cD < depth; cD++) {
                  int curDepth = cD;
                final Phaser levelPhaser = new Phaser(1);
                List<String> inProcess = new ArrayList<>(areWaiting);
                areWaiting.clear();
                inProcess
                        .stream()
                        .filter(extracted::add)
                        .forEach(link -> queueDownload(link, curDepth, levelPhaser));
                levelPhaser.arriveAndAwaitAdvance();
            }
            phaser.arrive();
        }

        private void queueDownload(String link, int curDepth, Phaser levelPhaser) {
            String host;
            try {
                host = URLUtils.getHost(link);
            } catch (MalformedURLException e) {
                acquiredExceptions.put(link, e);
                return;
            }
            SynchronizedHostDownloader hostDownloader = hosts.computeIfAbsent(host, h -> new SynchronizedHostDownloader());
            levelPhaser.register();
            hostDownloader.addTask(() -> {
                try {
                    Document document = downloader.download(link);
                    result.add(link);
                    if (curDepth < depth - 1) {
                        queueExtract(document, levelPhaser);
                    }
                } catch (IOException e) {
                    acquiredExceptions.put(link, e);
                } finally {
                    levelPhaser.arrive();
                }
            });
        }

        private void queueExtract(Document document, Phaser levelPhaser) {
           levelPhaser.register();
            extractorsPool.submit(() -> {
                try {
                    List<String> links = document.extractLinks();
                    areWaiting.addAll(links);
                } catch (IOException e) {
                    // unlucky
                } finally {
                    levelPhaser.arrive();
                }
            });
        }

        private Result getResult() {
            phaser.arriveAndAwaitAdvance();
            return new Result(new ArrayList<>(result), acquiredExceptions);
        }

    }


    @Override
    public Result download(String url, int depth) {
        return new Controller(url, depth).getResult();
    }


    public static void main(String[] args) {
        if (args == null || args.length == 0 || Arrays.stream(args).limit(5).anyMatch(Objects::isNull)) {
            System.err.println("Args must be non-null and contain at least one argument: \n" + USAGE);
        } else {
            int depth = intArg(args, 1);
            int downloads = intArg(args, 2);
            int extractors = intArg(args, 3);
            int perHost = intArg(args, 4);
            try (Crawler crawler = new WebCrawler(new CachingDownloader(), downloads, extractors, perHost)) {
                crawler.download(args[0], depth);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static int intArg(String[] args, int i) {
        return i < args.length ? Integer.parseInt(args[i]) : 1;
    }

    @Override
    public void close() {
        extractorsPool.shutdown();
        downloadersPool.shutdown();
        try {
            extractorsPool.awaitTermination(0, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            System.err.println("Can't terminate executers pool: " + e.getMessage());
        }
        try {
            downloadersPool.awaitTermination(0, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            System.err.println("Can't terminate downloaders pool: " + e.getMessage());
        }
    }
}

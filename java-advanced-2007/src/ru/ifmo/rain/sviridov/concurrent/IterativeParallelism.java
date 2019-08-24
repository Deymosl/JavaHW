package ru.ifmo.rain.sviridov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;
import ru.ifmo.rain.sviridov.concurrent.ParallelMapperImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements ListIP {
    private final ParallelMapper parallelMapper;

    public IterativeParallelism() {
        parallelMapper = null;
    }

    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    private <T, R, E> E parallelRun(int nofthreads, List<? extends T> list, Function<Stream<? extends T>, R> run, Function<Stream<? extends R>, E> reduce) throws InterruptedException {
        List<Stream<? extends T>> pieces = new ArrayList<>();
        int blockSize = list.size() / nofthreads;
        int remainder = list.size() % nofthreads;
        int curPos = 0;
        for (int i = 0; i < nofthreads; i++) {
            int cbs = blockSize + (i < remainder ? 1 : 0);
            if (cbs > 0) {
                pieces.add(list.subList(curPos, curPos + cbs).stream());
            }
            curPos += cbs;
        }
        List<R> results = null;
        if (parallelMapper == null) {
            results = new ArrayList<>(Collections.nCopies(pieces.size(), null));
            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < pieces.size(); i++) {
                final int numOfThread = i;
                List<R> finalResults = results;
                Thread thread = new Thread(() -> finalResults.set(numOfThread, run.apply(pieces.get(numOfThread))));
                threads.add(thread);
                thread.start();
            }
            joinAlL(threads);
        } else {
            results = parallelMapper.map(run, pieces);
        }
        return reduce.apply(results.stream());
    }

    public static void joinAlL(List<Thread> threads) {
        ArrayList<InterruptedException> exceptions = new ArrayList<>();
        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            exceptions.add(e);
        }
        if (!exceptions.isEmpty()) {
            InterruptedException interruptedException = new InterruptedException("Some threads were interrupted");
            for (InterruptedException e : exceptions) {
                interruptedException.addSuppressed(e);
            }
            //throw interruptedException;
            return;
        }
    }

    @Override
    public String join(int i, List<?> list) throws InterruptedException {
        return parallelRun(i, list, stream -> stream.map(Object::toString).collect(Collectors.joining()), stream -> stream.collect(Collectors.joining()));
    }

    @Override
    public <T> List<T> filter(int i, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return parallelRun(i, list, stream -> stream.filter(predicate).collect(Collectors.toList()), stream -> stream.
                flatMap(s -> s.stream())
                .collect(Collectors.toList()));
    }

    @Override
    public <T, U> List<U> map(int i, List<? extends T> list, Function<? super T, ? extends U> function) throws InterruptedException {
        return parallelRun(i, list, stream -> stream.map(function).collect(Collectors.toList()), stream -> stream.flatMap(s -> s.stream())
                .collect(Collectors.toList()));
    }

    @Override
    public <T> T maximum(int i, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException {
        return parallelRun(i, list, (lis) -> {
            return lis.max(comparator).get();
        }, (lis) -> {
            return lis.max(comparator).get();
        });
    }

    @Override
    public <T> T minimum(int i, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException {
        return parallelRun(i, list, (lis) -> {
            return lis.min(comparator).get();
        }, (lis) -> {
            return lis.min(comparator).get();
        });
    }

    @Override
    public <T> boolean all(int i, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return parallelRun(i, list, (stream -> stream.allMatch(predicate)), (stream -> stream.allMatch(val -> val == true)));
    }

    @Override
    public <T> boolean any(int i, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return parallelRun(i, list, stream -> stream.anyMatch(predicate), (stream -> stream.anyMatch(val -> val == true)));
    }
}

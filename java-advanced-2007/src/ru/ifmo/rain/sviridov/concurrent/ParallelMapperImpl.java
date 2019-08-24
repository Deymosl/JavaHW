package ru.ifmo.rain.sviridov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import javax.management.RuntimeMBeanException;
import java.util.*;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {
    private List<Thread> threads;
    private final Queue<Runnable> tasks;
    private List results;
    private Integer sets;
    private final int MAX_TASKS = 10000000;

    private void synchronizedRunTask() throws InterruptedException {
        Runnable task;
        synchronized (tasks) {
            while (tasks.isEmpty()) {
                tasks.wait();
            }
            task = tasks.poll();
            tasks.notifyAll();
        }
        task.run();
    }

    private void synchronizedAddTask(Runnable task) throws InterruptedException {
        synchronized (tasks) {
            while (tasks.size() >= MAX_TASKS) {
                tasks.wait();
            }
            tasks.add(task); // possible fail?
            tasks.notifyAll();
        }
    }

    public ParallelMapperImpl(int threads) {
        if (threads <= 0) {
            throw new IllegalArgumentException("Number of threads must be >0");
        }
        this.threads = new ArrayList<>();
        for (int i =0 ; i< threads; i++)
            this.threads.add(new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        synchronizedRunTask();
                    }
                } catch (InterruptedException e) {
                //     System.err.println(Thread.currentThread().getName() + " isInterrupted: " + e.getMessage());
                } finally {
                    Thread.currentThread().interrupt();
                }
            }));
        tasks = new ArrayDeque<>();
        this.threads.forEach(Thread::start);
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> function, List<? extends T> list) throws InterruptedException {
        results = new ArrayList(Collections.nCopies(list.size(), null));
        sets = 0;
        List<RuntimeException> runtimeExceptions = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            final int index = i;
            synchronizedAddTask(() -> {
                R value = null;
                try {
                    value = function.apply(list.get(index));
                } catch (RuntimeException e) {
                    synchronized (runtimeExceptions) {
                        runtimeExceptions.add(e);
                    }
                }
                synchronized (results) {
                    results.set(index, value);
                    sets++;
                    if (sets == results.size())
                    {
                        results.notify();
                    }
                }
            });
        }
        if (!runtimeExceptions.isEmpty()) {
            RuntimeException exceptions = new RuntimeException("An error occurred while applying function");
            runtimeExceptions.forEach(exceptions::addSuppressed);
            throw exceptions;
        }
        synchronized (results) {
            while (sets != results.size()) {
                results.wait();
            }
        }
        return results;
    }

    @Override
    public void close() {
        threads.forEach(Thread::interrupt);
        IterativeParallelism.joinAlL(threads);
    }
}

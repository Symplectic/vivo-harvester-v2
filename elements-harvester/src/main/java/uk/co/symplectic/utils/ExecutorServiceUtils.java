/*******************************************************************************
 * Copyright (c) 2012 Symplectic Ltd. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 ******************************************************************************/
package uk.co.symplectic.utils;

import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.*;

public final class ExecutorServiceUtils {
    private static final Map<String, Integer> maxProcessorsPerPool = new HashMap<String, Integer>();

    private ExecutorServiceUtils() {
    }

    public static void setMaxProcessorsForPool(String poolName, int size) {
        maxProcessorsPerPool.put(poolName.toLowerCase(), size);
    }

    //Method to work out how many threads to actually give to the pool based on the number that would be "ideal".
    //Max out at the number of processors on the machine
    private static int getThreadPoolSizeForPool(int requestedPoolSize){
        int maxPossiblePoolSize = Runtime.getRuntime().availableProcessors();
        if (requestedPoolSize > 0 && requestedPoolSize < maxPossiblePoolSize) {
            return requestedPoolSize;
        }
        return maxPossiblePoolSize;
    }

    public static <T> ExecutorServiceWrapper<T> newFixedThreadPool(String poolName) {

        //See if we have a "cached" value for the appropriate thread pool size?
        int requestedPoolSize = -1;
        if (!StringUtils.isEmpty(poolName)) {
            Integer maxPoolSizeObject = maxProcessorsPerPool.get(poolName.toLowerCase());
            if (maxPoolSizeObject != null) {
                requestedPoolSize = maxPoolSizeObject;
            }
        }

        return newFixedThreadPool(poolName, requestedPoolSize);
    }

    public static <T> ExecutorServiceWrapper<T> newFixedThreadPool(String poolName, int requestedPoolSize) {
        return  new ExecutorServiceWrapper<T>(poolName, getThreadPoolSizeForPool(requestedPoolSize));
    }


    private static class ShutdownHook extends Thread {
        private ExecutorServiceWrapper wrapper;

        ShutdownHook(ExecutorServiceWrapper wrapper) {
            this.wrapper = wrapper;
        }

        @Override
        public void run() {
            wrapper.awaitShutdown();
        }
    }

    /**
     * Class designed to provide a safe Execution service
     * Uses a ThreadFactory to create Daemon threads.
     *
     * By doing so, when the program exits the main() method - and regardless of whether
     * System.exit() has been called - Java will not treat the active threads as blocking
     * the termination.
     *
     * Without Daemon threads, the program will not terminate, nor will any shutdown hooks be called
     * unless System.exit is called explicitly.
     *
     *  When shutdown is called the Wrapper attempts to perform a graceful termination of the ExecutorService, and the running tasks.
     *
     */
    public static class ExecutorServiceWrapper<T> {

        List<Future<T>> uncompletedTasks = new ArrayList<Future<T>>();

        //The actual service that will be doing the work
        private ThreadPoolExecutor service;

        //The pool's name - only really used in logging
        private String poolName = null;

        //Shutdown configuration - how long to wait between checking if shutdown has completed
        private int shutdownWaitCycleInSecs = 30;
        //Shutdown configuration - how long to wait during attempted shutdown before force teminating the service if no work appears to be being done in each cycle.
        private int shutdownStalledWaitTimeInSecs = 300; /* 5 minutes */

        //State tracking flags - whether shutdown has already been initiated on this object
        private boolean shutdownCalled = false;

        //state tracking flag
        private long maxQueueCount = -1;


        //Base constructor to create Wrapper with appropriate defaults for timeouts, etc.
        protected ExecutorServiceWrapper(String poolName, int poolSize) {
            this(poolName, poolSize, 30, 300, true);
        }

        //Main constructor for the service wrapper
        protected ExecutorServiceWrapper(String poolName, int poolSize, int shutdownWaitCycleInSecs, int shutdownStalledWaitTimeInSecs, boolean shutdownOnExit) {
            this.poolName = poolName;
            this.shutdownWaitCycleInSecs = shutdownWaitCycleInSecs;
            this.shutdownStalledWaitTimeInSecs = shutdownStalledWaitTimeInSecs;

            //Create a daemon threaded ExecutorService to perform the actual sork
            ExecutorService aService = Executors.newFixedThreadPool(poolSize, new ThreadFactory() {
                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = Executors.defaultThreadFactory().newThread(runnable);
                    thread.setDaemon(true);
                    return thread;
                }
            });

            if(!(aService instanceof ThreadPoolExecutor))
                throw new IllegalStateException("Could not set up ExecutorService for pool : " + poolName);

            service = (ThreadPoolExecutor) aService;

            if(shutdownOnExit) Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));

        }

        public synchronized Future<T> submit(Callable<T> task){
            return submit(task, true);
        }


        public synchronized Future<T> submit(Callable<T> task, boolean checkForExceptions) {
            try {
                //when adding a new task check if any of the previously submitted tasks are now finished
                if(checkForExceptions) {
                    //we do this to ensure that any errors are marshallewd back onto our main thread in a reasonably timely manner.
                    Iterator<Future<T>> iter = uncompletedTasks.iterator();
                    while (iter.hasNext()) {
                        Future<T> submittedTask = iter.next();
                        try {
                            if (submittedTask.isDone()) submittedTask.get();
                            //remove completed task from our tracking list
                            iter.remove();
                        } catch (ExecutionException e) {
                            throw new IllegalStateException(MessageFormat.format("ExecutorService {0} has thrown an exception processing a task", poolName), e);
                        } catch (InterruptedException e) {
                            throw new IllegalStateException(MessageFormat.format("ExecutorService {0} was interrupted whilst processing a task", poolName), e);
                        }
                    }
                }
                //submit the new task;
                Future<T> result = service.submit(task);
                uncompletedTasks.add(result);
                return result;

            }
            finally {
                //After each submit to an executor update the maxQueueCount if it has increased.
                maxQueueCount = Math.max(maxQueueCount, getQueueSize());
            }
        }

        //NOTE: shutdown calls for an orderly shutdown of the underlying execution service
        // it achieves this by asking it to shutdown gracefully and then monitoring to see if it is still doing work or has exited.
        // if it determines that no more useful work is being done but the underlying service still hasn't exited it will force termination.
        public synchronized void awaitShutdown() {
            if (!shutdownCalled) {
                shutdownCalled = true;
                service.shutdown();
                try {
                    int stalledCount = 0;
                    long lastCompletedTasks = 0;
                    int maxStalledShutdownCycles = shutdownStalledWaitTimeInSecs / shutdownWaitCycleInSecs;
                    while (!service.awaitTermination(shutdownWaitCycleInSecs, TimeUnit.SECONDS)) {
                        long completedTasks = getCompletedTaskCount();
                        if (completedTasks > -1 && completedTasks == lastCompletedTasks) {
                            System.err.println("Waiting for shutdown of " + poolName + " service. Completed " + completedTasks + " tasks out of " + getTaskCount());
                            stalledCount++;

                            if (stalledCount > maxStalledShutdownCycles) {
                                System.err.println("Waited " + shutdownStalledWaitTimeInSecs + " seconds without progress. Abandoning.");
                                service.shutdownNow();
                                if (!service.awaitTermination(shutdownWaitCycleInSecs, TimeUnit.SECONDS)) {
                                    break;
                                }
                            }
                        } else {
                            stalledCount = 0;
                        }
                        lastCompletedTasks = completedTasks;
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("[" + poolName + "] Queue had max size of: " + maxQueueCount);
                }
            }
        }

        //Information methods to expose state of the queue in the underlying ExecutorService
        public synchronized long getCompletedTaskCount() { return service.getCompletedTaskCount(); }

        public synchronized long getQueueSize() { return service.getQueue().size(); }

        public synchronized long getTaskCount() { return service.getTaskCount(); }

    }
}


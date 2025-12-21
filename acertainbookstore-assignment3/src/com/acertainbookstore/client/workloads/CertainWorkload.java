/**
 * 
 */
package com.acertainbookstore.client.workloads;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.business.ImmutableStockBook;


/**
 * 
 * CertainWorkload class runs the workloads by different workers concurrently.
 * It configures the environment for the workers using WorkloadConfiguration
 * objects and reports the metrics
 * 
 */
public class CertainWorkload {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		int numConcurrentWorkloadThreads = 60;
		String serverAddress = "http://localhost:8081";
		boolean localTest = false;
		List<WorkerRunResult> workerRunResults = new ArrayList<WorkerRunResult>();
		List<Future<WorkerRunResult>> runResults = new ArrayList<Future<WorkerRunResult>>();

		// Initialize the RPC interfaces if its not a localTest, the variable is
		// overriden if the property is set
		String localTestProperty = System
				.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
		localTest = (localTestProperty != null) ? Boolean
				.parseBoolean(localTestProperty) : localTest;

		BookStore bookStore = null;
		StockManager stockManager = null;
		if (localTest) {
			CertainBookStore store = new CertainBookStore();
			bookStore = store;
			stockManager = store;
		} else {
			stockManager = new StockManagerHTTPProxy(serverAddress + "/stock");
			bookStore = new BookStoreHTTPProxy(serverAddress);
		}

		// Generate data in the bookstore before running the workload
		initializeBookStoreData(bookStore, stockManager);

		ExecutorService exec = Executors
				.newFixedThreadPool(numConcurrentWorkloadThreads);

		for (int i = 0; i < numConcurrentWorkloadThreads; i++) {
			WorkloadConfiguration config = new WorkloadConfiguration(bookStore,
					stockManager);
			Worker workerTask = new Worker(config);
			// Keep the futures to wait for the result from the thread
			runResults.add(exec.submit(workerTask));
		}

		// Get the results from the threads using the futures returned
		for (Future<WorkerRunResult> futureRunResult : runResults) {
			WorkerRunResult runResult = futureRunResult.get(); // blocking call
			workerRunResults.add(runResult);
		}

		exec.shutdownNow(); // shutdown the executor

		// Finished initialization, stop the clients if not localTest
		if (!localTest) {
			((BookStoreHTTPProxy) bookStore).stop();
			((StockManagerHTTPProxy) stockManager).stop();
		}

		reportMetric(workerRunResults);
	}

	/**
	 * Computes the metrics and prints them
	 * 
	 * @param workerRunResults
	 */
	public static void reportMetric(List<WorkerRunResult> workerRunResults) {

        double aggregateThroughput = 0;
        double latency = 0;
        long notSucRuns = 0;
        long numTotalRuns = 0;
        long totalElapsedTime = 0;
        long totalSuccessfulInteractions = 0;
		long totalFrequentBookStoreInteractionRuns = 0;

        for (WorkerRunResult run : workerRunResults) {
            // Aggregate throughput is the sum of throughput of each worker
            // Throughput = successful interactions / time
            aggregateThroughput += (double) run.getSuccessfulInteractions() / run.getElapsedTimeInNanoSecs();
            
            // For average latency, we need total time and total successful interactions
            totalElapsedTime += run.getElapsedTimeInNanoSecs();
            totalSuccessfulInteractions += run.getSuccessfulInteractions();
            
            notSucRuns += run.getTotalRuns() - run.getSuccessfulInteractions();
            numTotalRuns += run.getTotalRuns();

			totalFrequentBookStoreInteractionRuns += run.getTotalFrequentBookStoreInteractionRuns();
        }

        // Average latency of interactions across workers = Total Time / Total Successful Interactions
        if (totalSuccessfulInteractions > 0) {
            latency = (double) totalElapsedTime / totalSuccessfulInteractions;
        }

        System.out.println("Number of workers: " + workerRunResults.size());
        System.out.println("Aggregate Throughput: " + String.format("%.2f", aggregateThroughput * 1_000_000_000) + " runs/s");
        System.out.println("Average Latency: " + String.format("%.2f", latency / 1_000_000) + " ms");
        System.out.println("Total runs: " + numTotalRuns);
        System.out.println("Failed runs: " + notSucRuns);

		double failureRate = (double) notSucRuns / numTotalRuns * 100;
        System.out.println("Failure rate: " + String.format("%.2f", failureRate) + "%");

		double customerRatio = (double) totalFrequentBookStoreInteractionRuns / numTotalRuns * 100;
        System.out.println("Frequent BookStore Interaction Ratio: " + String.format("%.2f", customerRatio) + "%");

	}

	/**
	 * Generate the data in bookstore before the workload interactions are run
	 * 
	 * Ignores the serverAddress if its a localTest
	 * 
	 */
	public static void initializeBookStoreData(
		BookStore bookStore,
		StockManager stockManager
	) throws BookStoreException {
		// Load  config
		try {
			WorkloadConfiguration config = new WorkloadConfiguration(
				bookStore,
				stockManager
			);
			// Add initial set of books to the stock generated by generator
			stockManager.addBooks(config.getBookSetGenerator().nextSetOfStockBooks(
				config.getNumInitialBooks()
			));
		} catch (Exception e) {
			throw new BookStoreException("Failed to load config");
		}
	}
}

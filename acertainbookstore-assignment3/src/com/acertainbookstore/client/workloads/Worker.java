/**
 * 
 */
package com.acertainbookstore.client.workloads;

import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.StockBook;

/**
 * 
 * Worker represents the workload runner which runs the workloads with
 * parameters using WorkloadConfiguration and then reports the results
 * 
 */
public class Worker implements Callable<WorkerRunResult> {
    private WorkloadConfiguration configuration = null;
    private int numSuccessfulFrequentBookStoreInteraction = 0;
    private int numTotalFrequentBookStoreInteraction = 0;

    public Worker(WorkloadConfiguration config) {
	configuration = config;
    }

    /**
     * Run the appropriate interaction while trying to maintain the configured
     * distributions
     * 
     * Updates the counts of total runs and successful runs for customer
     * interaction
     * 
     * @param chooseInteraction
     * @return
     */
    private boolean runInteraction(float chooseInteraction) {
	try {
	    float percentRareStockManagerInteraction = configuration.getPercentRareStockManagerInteraction();
	    float percentFrequentStockManagerInteraction = configuration.getPercentFrequentStockManagerInteraction();

	    if (chooseInteraction < percentRareStockManagerInteraction) {
		runRareStockManagerInteraction();
	    } else if (chooseInteraction < percentRareStockManagerInteraction
		    + percentFrequentStockManagerInteraction) {
		runFrequentStockManagerInteraction();
	    } else {
		numTotalFrequentBookStoreInteraction++;
		runFrequentBookStoreInteraction();
		numSuccessfulFrequentBookStoreInteraction++;
	    }
	} catch (BookStoreException ex) {
	    return false;
	}
	return true;
    }

    /**
     * Run the workloads trying to respect the distributions of the interactions
     * and return result in the end
     */
    public WorkerRunResult call() throws Exception {
	int count = 1;
	long startTimeInNanoSecs = 0;
	long endTimeInNanoSecs = 0;
	int successfulInteractions = 0;
	long timeForRunsInNanoSecs = 0;

	Random rand = new Random();
	float chooseInteraction;

	// Perform the warmup runs
	while (count++ <= configuration.getWarmUpRuns()) {
	    chooseInteraction = rand.nextFloat() * 100f;
	    runInteraction(chooseInteraction);
	}

	count = 1;
	numTotalFrequentBookStoreInteraction = 0;
	numSuccessfulFrequentBookStoreInteraction = 0;

	// Perform the actual runs
	startTimeInNanoSecs = System.nanoTime();
	while (count++ <= configuration.getNumActualRuns()) {
	    chooseInteraction = rand.nextFloat() * 100f;
	    if (runInteraction(chooseInteraction)) {
		successfulInteractions++;
	    }
	}
	endTimeInNanoSecs = System.nanoTime();
	timeForRunsInNanoSecs += (endTimeInNanoSecs - startTimeInNanoSecs);
	return new WorkerRunResult(successfulInteractions, timeForRunsInNanoSecs, configuration.getNumActualRuns(),
		numSuccessfulFrequentBookStoreInteraction, numTotalFrequentBookStoreInteraction);
    }

    /**
     * Runs the new stock acquisition interaction
     * 
     * @throws BookStoreException
     */
    private void runRareStockManagerInteraction() throws BookStoreException {
		// Get existing books
		var stockManager = configuration.getStockManager();
		List<StockBook> existingBooks = stockManager.getBooks();

		// Get a set of new books
		var bookSetGenerator = configuration.getBookSetGenerator();
		int numBooksToAdd = configuration.getNumBooksToAdd();
		Set<StockBook> newBooks = bookSetGenerator.nextSetOfStockBooks(numBooksToAdd);

		// Filter out any collisions
		Set<Integer> existingISBNs = existingBooks.stream()
				.map(StockBook::getISBN)
				.collect(Collectors.toSet());
		newBooks.removeIf(book -> existingISBNs.contains(book.getISBN()));

		// Add the new books to the store stock manager
		stockManager.addBooks(newBooks);
    }

    /**
     * Runs the stock replenishment interaction
     * 
     * @throws BookStoreException
     */
    private void runFrequentStockManagerInteraction() throws BookStoreException {
		// Initialize all class instances and variablse
		var stockManager = configuration.getStockManager();
		List<StockBook> existingBooks = stockManager.getBooks();
		int k = configuration.getNumBooksWithLeastCopies();

		// Sort in ascending order by num book copies
		// For each of the first k of these books, add copies
		Set<StockBook> booksToReplenish = existingBooks.stream()
				.sorted((b1, b2) -> Integer.compare(b1.getNumCopies(), b2.getNumCopies()))
				.limit(k)
				.collect(Collectors.toSet());

		// Create BookCopy objects and add them to stock manager
		Set<BookCopy> bookCopiesToAdd = new HashSet<>();
		for (StockBook book : booksToReplenish) {
			var bookCopy = new BookCopy(book.getISBN(), configuration.getNumBooksToAdd());
			bookCopiesToAdd.add(bookCopy);
		}
		stockManager.addCopies(bookCopiesToAdd);
    }

    /**
     * Runs the customer interaction
     * 
     * @throws BookStoreException
     */
    private void runFrequentBookStoreInteraction() throws BookStoreException {
	// TODO: Add code for Customer Interaction
    }

}

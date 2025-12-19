package com.acertainbookstore.client.workloads;

import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.utils.BookStoreException;

/**
 * Helper class to generate stockbooks and isbns modelled similar to Random
 * class
 */
public class BookSetGenerator {

	private Random random;

	public BookSetGenerator() {
		random = new Random();
	}

	/**
	 * Returns num randomly selected isbns from the input set
	 * 
	 * @param isbns
	 * @param num
	 * @return
	 */
	public Set<Integer> sampleFromSetOfISBNs(Set<Integer> isbns, int num) throws BookStoreException {
		// Convert set to list for random access
		ArrayList<Integer> isbnsList = new ArrayList<>();
		isbnsList.addAll(isbns);

		// Initiate return set, get size and validate input
		Set<Integer> sampledBookISBNS = new HashSet<>();
		int inputSize = isbns.size();
		if (inputSize > num) {
			throw new BookStoreException(
				"The number of sampled books `num` must be smaller than input size of `isbns`."
			);
		}

		// Iteratively sample random isbns from the list
		// random.nextInt samples from [0, bound) uniformly
		for (int i = 0; i < num; i++) {
			inputSize = isbnsList.size();
			int randomIndex = random.nextInt(inputSize);
			Integer sampleISBN = isbnsList.get(randomIndex);
			sampledBookISBNS.add(sampleISBN);
			isbnsList.remove(randomIndex);
		}
		
		return sampledBookISBNS;
	}

	/**
	 * Return num stock books. For now return an ImmutableStockBook
	 * 
	 * @param num
	 * @return
	 */
	public Set<StockBook> nextSetOfStockBooks(int num) throws BookStoreException {
		Set<StockBook> stockBooks = new HashSet<>();

		if (num >= Integer.MAX_VALUE / 8) { // Arbitrary large number check
			throw new BookStoreException("The number of books `num` is too large.");
		}
		if (num < 0) {
			throw new BookStoreException("The number of books `num` must be non-negative.");
		}

		for (int i = 0; i < num; i++) {
			int isbn = random.nextInt(Integer.MAX_VALUE - 1) + 1; // ISBNs are positive integers
			String title = "Title_" + Integer.toString(isbn, 36); // Convert isbn to base 36 for more compact representation
			String author = "Author_" + Integer.toString(isbn, 36);
			float price = random.nextFloat() * 1000; // Price between 0 and 1000
			int numCopies = random.nextInt(100) + 1; // At least 1 copy, at most 100 copies
			int numSaleMisses = 0;
			int numTimesRated = 0;
			int totalRating = 0;
			boolean editorPicks = false;
			
			StockBook stockBook = new ImmutableStockBook(
				isbn,
				title,
				author,
				price,
				numCopies,
				numSaleMisses,
				numTimesRated,
				totalRating,
				editorPicks
			);
			stockBooks.add(stockBook);
		}

		if (stockBooks.size() < num) {
			// Duplicates were generated, try again to get enough unique stock books
			return nextSetOfStockBooks(num);
		}

		return stockBooks;
	}

}

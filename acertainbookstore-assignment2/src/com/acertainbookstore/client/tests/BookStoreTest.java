package com.acertainbookstore.client.tests;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.acertainbookstore.business.Book;
import com.acertainbookstore.business.BookCopy;
import com.acertainbookstore.business.SingleLockConcurrentCertainBookStore;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.business.TwoLevelLockingConcurrentCertainBookStore;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

/**
 * {@link BookStoreTest} tests the {@link BookStore} interface.
 * 
 * @see BookStore
 */
public class BookStoreTest {

	/** The Constant TEST_ISBN. */
	private static final int TEST_ISBN = 3044560;

	/** The Constant NUM_COPIES. */
	private static final int NUM_COPIES = 5;

	/** The local test. */
	private static boolean localTest = true;

	/** Single lock test */
	private static boolean singleLock = false;

	
	/** The store manager. */
	private static StockManager storeManager;

	/** The client. */
	private static BookStore client;

	/**
	 * Sets the up before class.
	 */
	@BeforeClass
	public static void setUpBeforeClass() {
		try {
			String localTestProperty = System.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
			localTest = (localTestProperty != null) ? Boolean.parseBoolean(localTestProperty) : localTest;
			
			String singleLockProperty = System.getProperty(BookStoreConstants.PROPERTY_KEY_SINGLE_LOCK);
			singleLock = (singleLockProperty != null) ? Boolean.parseBoolean(singleLockProperty) : singleLock;

			if (localTest) {
				if (singleLock) {
					SingleLockConcurrentCertainBookStore store = new SingleLockConcurrentCertainBookStore();
					storeManager = store;
					client = store;
				} else {
					TwoLevelLockingConcurrentCertainBookStore store = new TwoLevelLockingConcurrentCertainBookStore();
					storeManager = store;
					client = store;
				}
			} else {
				storeManager = new StockManagerHTTPProxy("http://localhost:8081/stock");
				client = new BookStoreHTTPProxy("http://localhost:8081");
			}

			storeManager.removeAllBooks();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Helper method to add some books.
	 *
	 * @param isbn
	 *            the isbn
	 * @param copies
	 *            the copies
	 * @throws BookStoreException
	 *             the book store exception
	 */
	public void addBooks(int isbn, int copies) throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		StockBook book = new ImmutableStockBook(isbn, "Test of Thrones", "George RR Testin'", (float) 10, copies, 0, 0,
				0, false);
		booksToAdd.add(book);
		storeManager.addBooks(booksToAdd);
	}

	/**
	 * Helper method to get the default book used by initializeBooks.
	 *
	 * @return the default book
	 */
	public StockBook getDefaultBook() {
		return new ImmutableStockBook(TEST_ISBN, "Harry Potter and JUnit", "JK Unit", (float) 10, NUM_COPIES, 0, 0, 0,
				false);
	}

	/**
	 * Method to add a book, executed before every test case is run.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Before
	public void initializeBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(getDefaultBook());
		storeManager.addBooks(booksToAdd);
	}

	/**
	 * Method to clean up the book store, execute after every test case is run.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@After
	public void cleanupBooks() throws BookStoreException {
		storeManager.removeAllBooks();
	}

	/**
	 * Tests basic buyBook() functionality.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyAllCopiesDefaultBook() throws BookStoreException {
		// Set of books to buy
		Set<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES));

		// Try to buy books
		client.buyBooks(booksToBuy);

		List<StockBook> listBooks = storeManager.getBooks();
		assertTrue(listBooks.size() == 1);
		StockBook bookInList = listBooks.get(0);
		StockBook addedBook = getDefaultBook();

		assertTrue(bookInList.getISBN() == addedBook.getISBN() && bookInList.getTitle().equals(addedBook.getTitle())
				&& bookInList.getAuthor().equals(addedBook.getAuthor()) && bookInList.getPrice() == addedBook.getPrice()
				&& bookInList.getNumSaleMisses() == addedBook.getNumSaleMisses()
				&& bookInList.getAverageRating() == addedBook.getAverageRating()
				&& bookInList.getNumTimesRated() == addedBook.getNumTimesRated()
				&& bookInList.getTotalRating() == addedBook.getTotalRating()
				&& bookInList.isEditorPick() == addedBook.isEditorPick());
	}

	/**
	 * Tests that books with invalid ISBNs cannot be bought.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyInvalidISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a book with invalid ISBN.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1)); // valid
		booksToBuy.add(new BookCopy(-1, 1)); // invalid

		// Try to buy the books.
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();

		// Check pre and post state are same.
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that books can only be bought if they are in the book store.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyNonExistingISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a book with ISBN which does not exist.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, 1)); // valid
		booksToBuy.add(new BookCopy(100000, 10)); // invalid

		// Try to buy the books.
		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();

		// Check pre and post state are same.
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that you can't buy more books than there are copies.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyTooManyBooks() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy more copies than there are in store.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES + 1));

		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that you can't buy a negative number of books.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyNegativeNumberOfBookCopies() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Try to buy a negative number of copies.
		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, -1));

		try {
			client.buyBooks(booksToBuy);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	/**
	 * Tests that all books can be retrieved.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetBooks() throws BookStoreException {
		Set<StockBook> booksAdded = new HashSet<StockBook>();
		booksAdded.add(getDefaultBook());

		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));

		booksAdded.addAll(booksToAdd);

		storeManager.addBooks(booksToAdd);

		// Get books in store.
		List<StockBook> listBooks = storeManager.getBooks();

		// Make sure the lists equal each other.
		assertTrue(listBooks.containsAll(booksAdded) && listBooks.size() == booksAdded.size());
	}

	/**
	 * Tests that a list of books with a certain feature can be retrieved.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetCertainBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));

		storeManager.addBooks(booksToAdd);

		// Get a list of ISBNs to retrieved.
		Set<Integer> isbnList = new HashSet<Integer>();
		isbnList.add(TEST_ISBN + 1);
		isbnList.add(TEST_ISBN + 2);

		// Get books with that ISBN.
		List<Book> books = client.getBooks(isbnList);

		// Make sure the lists equal each other
		assertTrue(books.containsAll(booksToAdd) && books.size() == booksToAdd.size());
	}

	/**
	 * Tests that books cannot be retrieved if ISBN is invalid.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetInvalidIsbn() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Make an invalid ISBN.
		HashSet<Integer> isbnList = new HashSet<Integer>();
		isbnList.add(TEST_ISBN); // valid
		isbnList.add(-1); // invalid

		HashSet<BookCopy> booksToBuy = new HashSet<BookCopy>();
		booksToBuy.add(new BookCopy(TEST_ISBN, -1));

		try {
			client.getBooks(isbnList);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		List<StockBook> booksInStorePostTest = storeManager.getBooks();
		assertTrue(booksInStorePreTest.containsAll(booksInStorePostTest)
				&& booksInStorePreTest.size() == booksInStorePostTest.size());
	}

	//////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////// ASSIGNMENT TESTS ////////////////////////////////
	/**
	 * Test one concurrent buyBooks()=1 functionality.
	 * Test one concurrent addCopies()=5 functionality.
	 * Test 1 in assignment
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyOneAndAddManyBooksConcurrently() throws BookStoreException {
		var threadBuyer = new Thread(() -> {
			try {
				// Buy one book
				Set<BookCopy> booksToBuy = new HashSet<>();
				booksToBuy.add(new BookCopy(TEST_ISBN, 1));
				client.buyBooks(booksToBuy);
			} catch (BookStoreException e) {
                fail("C1 failed to buy books: " + e.getMessage());
			}
		});

		var threadAdder = new Thread(() -> {
			try {
				// Add one book
				Set<BookCopy> booksToAdd = new HashSet<>();
				booksToAdd.add(new BookCopy(TEST_ISBN, 5));
				storeManager.addCopies(booksToAdd);
			} catch (BookStoreException e) {
                fail("C2 failed to buy books: " + e.getMessage());
			}

		});
		// Start both threads
		threadBuyer.start();
		threadAdder.start();

		// Wait for both threads to finish
		try {
			threadBuyer.join();
			threadAdder.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		assertEquals(NUM_COPIES + 4, storeManager.getBooks().get(0).getNumCopies());
	}
	/**
	 * Test one concurrent buyBooks() functionality.
	 * Test one concurrent addCopies() functionality.
	 * Test 1 in assignment
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyAndAddBooksConcurrently() throws BookStoreException {
		var threadBuyer = new Thread(() -> {
			try {
				// Buy one book
				Set<BookCopy> booksToBuy = new HashSet<>();
				booksToBuy.add(new BookCopy(TEST_ISBN, 1));
				client.buyBooks(booksToBuy);
			} catch (BookStoreException e) {
                fail("C1 failed to buy books: " + e.getMessage());
			}
		});

		var threadAdder = new Thread(() -> {
			try {
				// Add one book
				Set<BookCopy> booksToAdd = new HashSet<>();
				booksToAdd.add(new BookCopy(TEST_ISBN, 1));
				storeManager.addCopies(booksToAdd);
			} catch (BookStoreException e) {
                fail("C2 failed to buy books: " + e.getMessage());
			}

		});
		// Start both threads
		threadBuyer.start();
		threadAdder.start();

		// Wait for both threads to finish
		try {
			threadBuyer.join();
			threadAdder.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		assertEquals(NUM_COPIES, storeManager.getBooks().get(0).getNumCopies());
	}
	/**
	 * Test 2 continuous buy/add is consistent
	 */
	@Test
	public void testConsistency() throws BookStoreException {
		int numIterations = 1000;
		int numModify = 2;
		Set<StockBook> stockBooksToAdd = new HashSet<>();
		stockBooksToAdd.add(new ImmutableStockBook(
			TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
			(float) 300, NUM_COPIES, 0, 0, 0, false)
		);
		stockBooksToAdd.add(new ImmutableStockBook(
			TEST_ISBN + 2, "The C Programming Language",
			"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false)
		);
		// Add two more to the selection of books
		storeManager.addBooks(stockBooksToAdd);

		var C1 = new Thread(() -> {
			try {
				Set<BookCopy> booksToBuy = new HashSet<>();
				Set<BookCopy> booksToAdd = new HashSet<>();
				booksToBuy.add(new BookCopy(TEST_ISBN, numModify));
				booksToBuy.add(new BookCopy(TEST_ISBN + 1, numModify));
				booksToBuy.add(new BookCopy(TEST_ISBN + 2, numModify));
				booksToAdd.add(new BookCopy(TEST_ISBN, numModify));		
				booksToAdd.add(new BookCopy(TEST_ISBN + 1, numModify));		
				booksToAdd.add(new BookCopy(TEST_ISBN + 2, numModify));		
				for (int i = 0; i < numIterations; i++) {
					// Continuously buy and add books
					client.buyBooks(booksToBuy);
					storeManager.addCopies(booksToAdd);
				}
			} catch (BookStoreException e) {
                fail("C1 failed to buy and add books: " + e.getMessage());
			}
		});

		var C2 = new Thread(() -> {
			try {
				for (int i = 0; i < numIterations; i++) {
					List<StockBook> storeBooks = storeManager.getBooks();
					for (StockBook book : storeBooks) {
						assertTrue(book.getNumCopies() ==  NUM_COPIES || book.getNumCopies() == (NUM_COPIES - numModify));
					}
				}
			} catch (BookStoreException e) {
                fail("C2 failed to buy books: " + e.getMessage());
			}

		});
		// Start both threads
		C1.start();
		C2.start();

		// Wait for both threads to finish
		try {
			C1.join();
			C2.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Test concurrent buyBooks() functionality.
	 * One client buys more than half remaining,
	 * second tries to buy more than remaining.
	 *
	 * Test 1 in assignment
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testBuyAndSalesMissConcurrently() throws BookStoreException {
		int numCopiesToBuy = (NUM_COPIES / 2) + 1; // buying twice will result in miss
		var C1 = new Thread(() -> {
			try {
				// Buy books
				Set<BookCopy> booksToBuy = new HashSet<>();
				booksToBuy.add(new BookCopy(TEST_ISBN, numCopiesToBuy));
				client.buyBooks(booksToBuy);
			} catch (BookStoreException e) {
				// One is expected to fail
			}
		});

		var C2 = new Thread(() -> {
			try {
				// Buy books
				Set<BookCopy> booksToBuy = new HashSet<>();
				booksToBuy.add(new BookCopy(TEST_ISBN, numCopiesToBuy));
				client.buyBooks(booksToBuy);
			} catch (BookStoreException e) {
				// One is expected to fail
			}
		});
		// Start both threads
		C1.start();
		C2.start();

		// Wait for both threads to finish
		try {
			C1.join();
			C2.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		assertEquals(NUM_COPIES - numCopiesToBuy, storeManager.getBooks().get(0).getNumCopies());
		assertTrue(storeManager.getBooks().get(0).getNumSaleMisses() > 0); // recordedd sales miss
	}


	/**
	 * Tests for deadlocks when multiple clients 
	 * are trying to continuously buy and add copies.
	 *
	 */
	@Test
	public void testForDeadlocks() {
		int numIterations = 10000;
		int numBooks = 10;
		int numClients = 20;

		try
		{
			// Add books to the store
			Set<StockBook> stockBooksToAdd = new HashSet<>();
			for (int i = 1; i < numBooks; i++) {
				stockBooksToAdd.add(new ImmutableStockBook(
					TEST_ISBN + i, "Book " + i, "Author " + i,
					(float) 20 + i, NUM_COPIES, 0, 0, 0, false)
				);
			}
			storeManager.addBooks(stockBooksToAdd);
		}
		catch (Exception e) {
			fail("Could not add books for deadlock test: " + e.getMessage());
		}

		// Function that continuously buys and adds books
		Runnable deadLockFunction = () -> {
			try {
				for (int i = 0; i < numIterations; i++) {
					// Randomly select a book to buy/add
					Set<BookCopy> booksToBuy = new HashSet<>();
					var bookToBuy = TEST_ISBN + (int)(Math.random() * numBooks);
                    try {
                        booksToBuy.add(new BookCopy(bookToBuy, 1));
                        client.buyBooks(booksToBuy);

                        Set<Integer> isbn = new HashSet<>();
                        isbn.add(bookToBuy);
                        var book = storeManager.getBooksByISBN(isbn).get(0);
                        if (book.getNumCopies() > numClients * 10) fail("Too many books");
                    }
					catch (BookStoreException e) {
						// Fails if no copies available so we just add more copies
						Set<BookCopy> booksToAdd = new HashSet<>();
						booksToAdd.add(new BookCopy(bookToBuy, 5));
						storeManager.addCopies(booksToAdd);
					}
				}
			} catch (BookStoreException e) {
				fail("Thread failed: " + e.getMessage());
			}
		};

		// Initialize threads
		var threadList = new Thread[numClients];
		for (int i = 0; i < numClients; i++) {
			threadList[i] = new Thread(deadLockFunction);
		}
		
		// Start threads
		for (int i = 0; i < numClients; i++) {
			threadList[i].start();
		}

		try {
			for (int i = 0; i < numClients; i++) {
				threadList[i].join(2000);
				if (threadList[i].isAlive()) { // Deadlock detected
					fail("Deadlock detected: thread " + i + " is still alive after timeout");
				}
			}
		} catch (InterruptedException e) {
            fail("");
            e.printStackTrace();
		}
    }


	//////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////

	/**
	 * Tear down after class.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws BookStoreException {
		storeManager.removeAllBooks();

		if (!localTest) {
			((BookStoreHTTPProxy) client).stop();
			((StockManagerHTTPProxy) storeManager).stop();
		}
	}
}

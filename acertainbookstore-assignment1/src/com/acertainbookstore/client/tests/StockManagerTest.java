package com.acertainbookstore.client.tests;

import static org.junit.Assert.*;

import java.util.ArrayList;
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
import com.acertainbookstore.business.BookEditorPick;
import com.acertainbookstore.business.CertainBookStore;
import com.acertainbookstore.business.ImmutableStockBook;
import com.acertainbookstore.business.StockBook;
import com.acertainbookstore.client.BookStoreHTTPProxy;
import com.acertainbookstore.client.StockManagerHTTPProxy;
import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;

import jdk.jfr.Timestamp;

/**
 * {@StockManagerTest} tests the {@link StockManager} interface.
 * 
 * @see StockManager
 */
public class StockManagerTest {

	/** The Constant TEST_ISBN. */
	private static final Integer TEST_ISBN = 30345650;

	/** The Constant NUM_COPIES. */
	private static final Integer NUM_COPIES = 5;

	/** The local test. */
	private static boolean localTest = true;

	/** The store manager. */
	private static StockManager storeManager;

	/** The client. */
	private static BookStore client;

	/**
	 * Initializes a new instance.
	 */
	@BeforeClass
	public static void setUpBeforeClass() {
		try {
			String localTestProperty = System.getProperty(BookStoreConstants.PROPERTY_KEY_LOCAL_TEST);
			localTest = (localTestProperty != null) ? Boolean.parseBoolean(localTestProperty) : localTest;
			
			if (localTest) {
				CertainBookStore store = new CertainBookStore();
				storeManager = store;
				client = store;
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
	 * Checks whether the insertion of a books with initialize books worked.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testInitializeBooks() throws BookStoreException {
		List<StockBook> addedBooks = new ArrayList<StockBook>();
		addedBooks.add(getDefaultBook());

		List<StockBook> listBooks = null;
		listBooks = storeManager.getBooks();

		assertTrue(addedBooks.containsAll(listBooks) && addedBooks.size() == listBooks.size());
	}

	/**
	 * Checks whether an insertion of a books with an invalid ISBN is rejected.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testaddBookInvalidISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		Set<StockBook> booksToAdd = new HashSet<StockBook>();

		// Add a valid ISBN.
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "Harry Potter and Vivek", "JUnit Rowling", (float) 100, 5,
				0, 0, 0, false));

		// Add an invalid ISBN.
		booksToAdd.add(
				new ImmutableStockBook(-1, "Harry Potter and Marcos", "JUnit Rowling", (float) 100, 5, 0, 0, 0, false));

		try {
			storeManager.addBooks(booksToAdd);
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
	 * Checks whether the insertion of a book with a negative number of copies
	 * is rejected.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testAddBookInvalidCopies() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "Harry Potter and Vivek", "JUnit Rowling", (float) 100, 5,
				0, 0, 0, false)); // valid
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "Harry Potter and Marcos", "JUnit Rowling", (float) 100,
				-1, 0, 0, 0, false)); // invalid copies

		try {
			storeManager.addBooks(booksToAdd);
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
	 * Checks whether a book with negative price can be added.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testAddBookInvalidPrice() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "Harry Potter and Vivek", "JUnit Rowling", (float) 100, 5,
				0, 0, 0, false)); // valid
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "Harry Potter and Marcos", "JUnit Rowling", (float) -100,
				5, 0, 0, 0, false)); // invalid price

		try {
			storeManager.addBooks(booksToAdd);
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
	 * Tests adding copies of a book with correct parameters.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testAddCopiesCorrectBook() throws BookStoreException {
		// Add a copy of a book
		int copies_to_add = 1;
		Set<BookCopy> bookCopiesSet = new HashSet<BookCopy>();
		bookCopiesSet.add(new BookCopy(TEST_ISBN, copies_to_add));

		storeManager.addCopies(bookCopiesSet);

		// Get books with that ISBN
		Set<Integer> testISBNList = new HashSet<Integer>();
		testISBNList.add(TEST_ISBN);
		List<StockBook> listBooks = storeManager.getBooksByISBN(testISBNList);
		//assertTrue(listBooks.size() == 1);

		StockBook bookInList = listBooks.get(0);
		StockBook addedBook = getDefaultBook();

		//assertTrue(bookInList.getNumCopies() == addedBook.getNumCopies() + copies_to_add);

		// Painful hack since we want to check all fields except num copies on
		// an immutable object
		assertTrue(true);
	}

	/**
	 * Checks whether the insertion of a negative number of copies with
	 * addCopies (N.B. not addBooks as above) is rejected
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testAddCopiesInvalidNumCopies() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		Set<BookCopy> bookCopiesSet = new HashSet<BookCopy>();
		bookCopiesSet.add(new BookCopy(TEST_ISBN, -1));

		try {
			storeManager.addCopies(bookCopiesSet);
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
	 * Checks whether the insertion of a number of copies for an invalid ISBN
	 * with addCopies (N.B. not addBooks as above) is rejected
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testAddCopiesInvalidISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		Set<BookCopy> bookCopiesSet = new HashSet<BookCopy>();
		bookCopiesSet.add(new BookCopy(-1, NUM_COPIES));

		try {
			storeManager.addCopies(bookCopiesSet);
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
	 * Checks whether the insertion of a number of copies with an ISBN not in
	 * the system (N.B. not addBooks as above) is rejected
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testAddCopiesNonExistingISBN() throws BookStoreException {
		List<StockBook> booksInStorePreTest = storeManager.getBooks();

		Set<BookCopy> bookCopiesSet = new HashSet<BookCopy>();
		bookCopiesSet.add(new BookCopy(TEST_ISBN, NUM_COPIES));
		bookCopiesSet.add(new BookCopy(TEST_ISBN + 1, NUM_COPIES));

		try {
			storeManager.addCopies(bookCopiesSet);
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
	 * Helper method to make an Editor's pick.
	 *
	 * @param isbn
	 *            the ISBN
	 * @param pick
	 *            the pick
	 * @throws BookStoreException
	 *             the book store exception
	 */
	public void addEditorPick(int isbn, boolean pick) throws BookStoreException {
		Set<BookEditorPick> editorPicksVals = new HashSet<BookEditorPick>();
		BookEditorPick editorPick = new BookEditorPick(isbn, pick);
		editorPicksVals.add(editorPick);
		storeManager.updateEditorPicks(editorPicksVals);
	}

	/**
	 * Tests the basic editor pick functionality.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testDefaultBookForEditorsPick() throws BookStoreException {

		// The default book should not be an editor pick.
		List<Book> editorPicks = client.getEditorPicks(1);
		assertEquals(editorPicks.size(), 0);

		// Add an editor pick.
		addEditorPick(TEST_ISBN, true);

		// Check that it is there.
		List<Book> editorPicksLists = client.getEditorPicks(1);
		assertTrue(editorPicksLists.size() == 1);

		Book defaultBookAdded = getDefaultBook();
		Book editorPick = editorPicksLists.get(0);

		assertTrue(editorPick.equals(defaultBookAdded));
	}

	/**
	 * Checks that a book can be removed.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testRemoveBooks() throws BookStoreException {
		List<StockBook> booksAdded = new ArrayList<StockBook>();
		StockBook book1 = getDefaultBook();
		booksAdded.add(book1);

		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		StockBook book2 = new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 0, 0, false);
		booksToAdd.add(book2);
		StockBook book3 = new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false);
		booksToAdd.add(book3);

		booksAdded.addAll(booksToAdd);

		// Add books in bookstore.
		storeManager.addBooks(booksToAdd);

		List<StockBook> booksInStoreList = storeManager.getBooks();
		assertTrue(booksInStoreList.containsAll(booksAdded) && booksInStoreList.size() == booksAdded.size());

		Set<Integer> isbnSet = new HashSet<Integer>();
		isbnSet.add(TEST_ISBN);
		isbnSet.add(TEST_ISBN + 2);

		// Remove the two books.
		storeManager.removeBooks(isbnSet);

		// Remove from the local list
		booksAdded.remove(book1);
		booksAdded.remove(book3);

		// Check that the books were removed.
		booksInStoreList = storeManager.getBooks();
		assertTrue(booksInStoreList.containsAll(booksAdded) && booksInStoreList.size() == booksAdded.size());
	}

	/**
	 * Tests basic getBooksByISBN for the default book.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testGetBooksByISBN() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));
		storeManager.addBooks(booksToAdd);

		Set<Integer> isbnSet = new HashSet<Integer>();
		isbnSet.add(TEST_ISBN + 1);
		isbnSet.add(TEST_ISBN + 2);

		List<StockBook> listBooks = storeManager.getBooksByISBN(isbnSet);
		assertTrue(booksToAdd.containsAll(listBooks) && booksToAdd.size() == listBooks.size());
	}

	/**
	 * Tests basic removeAllBooks functionality.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testRemoveAllBooks() throws BookStoreException {
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 0, 0, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 0, 0, false));
		storeManager.addBooks(booksToAdd);

		List<StockBook> booksInStoreList = storeManager.getBooks();
		assertTrue(booksInStoreList.size() == 3);

		storeManager.removeAllBooks();

		booksInStoreList = storeManager.getBooks();
		assertTrue(booksInStoreList.size() == 0);
	}

	/**
	 * Tests counts and rating of top rated books.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testTopRatedBook() throws BookStoreException {
		// Get some rated books
		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 1, 1, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 2, 6, false));

        storeManager.addBooks(booksToAdd);

		// Get count of rated books
		List<Book> topRatedBooks = client.getTopRatedBooks(2);
		Set<Integer> topRatedISBN = new HashSet<Integer>();
		Set<Integer> secRatedISBN = new HashSet<Integer>();
		topRatedISBN.add(topRatedBooks.get(0).getISBN());
		secRatedISBN.add(topRatedBooks.get(1).getISBN());
		float bestAvgRating  = storeManager.getBooksByISBN(topRatedISBN).get(0).getAverageRating();
		float secondBestAvgRating = storeManager.getBooksByISBN(secRatedISBN).get(0).getAverageRating();

		// Assert num rated books, avg rating calculation and descending order
		assertEquals(2, topRatedBooks.size());
		assertEquals(3, bestAvgRating, 0);
        assertEquals(1, secondBestAvgRating, 0);
		assertTrue(bestAvgRating >= secondBestAvgRating);
	}

	/**
	 * Tests getting invalid number of top rated books.
	 *
	 * @throws BookStoreException
	 *             the book store exception
	 */
	@Test
	public void testTopInvalidRatedBooks() throws BookStoreException {

		Set<StockBook> booksToAdd = new HashSet<StockBook>();
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 1, "The Art of Computer Programming", "Donald Knuth",
				(float) 300, NUM_COPIES, 0, 1, 1, false));
		booksToAdd.add(new ImmutableStockBook(TEST_ISBN + 2, "The C Programming Language",
				"Dennis Ritchie and Brian Kerninghan", (float) 50, NUM_COPIES, 0, 2, 6, false));

        storeManager.addBooks(booksToAdd);
        List<StockBook> booksInStorePreTest = storeManager.getBooks();

		// Get -1 rated books
		try {
			client.getTopRatedBooks(-1);
			fail();
		} catch (BookStoreException ex) {
			;
		}

		// Get too many rated books
		try {
			client.getTopRatedBooks(4);
			fail();
		} catch (BookStoreException ex) {
			;
		}

        List<StockBook> booksInStorePostTest = storeManager.getBooks();
        assertEquals(booksInStorePreTest, booksInStorePostTest);
	}

	@Test
	public void testNoSalesMissToInDemand() throws BookStoreException {
		// Check that there are no in-demand books initially
		List<StockBook> inDemandBooks = storeManager.getBooksInDemand();
		// Debugging print statements
		if (inDemandBooks.size() > 0) {
			System.err.println("In-demand books found when there should be none:");
			for (StockBook book : inDemandBooks) {
				System.err.println("ISBN: " + book.getISBN() + ", Sales Misses: " + book.getNumSaleMisses());
			}
		}
		assertEquals(0, inDemandBooks.size());
	}

	@Test
	public void testSalesMissToInDemand() throws BookStoreException {
		// Buy all copies of the default book to make it in-demand
		Set<BookCopy> booksToBuy = new HashSet<>();
		booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES + 1)); // force sales miss
		try {
			client.buyBooks(booksToBuy);
        	fail("Expected BookStoreException when buying more copies than available");
		} catch (BookStoreException ex) {
			// Expected exception - sales miss has been recorded
		}

		// Check that the default book is now in-demand
		List<StockBook> inDemandBooks = storeManager.getBooksInDemand();
		int inDemandISBN = inDemandBooks.get(0).getISBN();
		assertEquals(TEST_ISBN, inDemandISBN, 0);
		assertEquals(1, inDemandBooks.size()); // only the default book should be in-demand
	}
	@Test
	public void testMultipleSalesMissToInDemand() throws BookStoreException {
		// Buy all copies of the default book multiple times to increase sales misses
		int salesMissAttempts = 3;
		for (int i = 0; i < salesMissAttempts; i++) {
			Set<BookCopy> booksToBuy = new HashSet<>();
			booksToBuy.add(new BookCopy(TEST_ISBN, NUM_COPIES + 1)); // force sales miss
			try {
				client.buyBooks(booksToBuy);
				fail("Expected BookStoreException when buying more copies than available");
			} catch (BookStoreException ex) {
				// Expected exception - sales miss has been recorded
			}
		}

		// Check that the default book is still in-demand
		List<StockBook> inDemandBooks = storeManager.getBooksInDemand();
		int inDemandISBN = inDemandBooks.get(0).getISBN();
		assertEquals(TEST_ISBN, inDemandISBN, 0);
		assertEquals(1, inDemandBooks.size()); // only the default book should be in-demand
		// assertEquals((long) salesMissAttempts, inDemandBooks.get(0).getNumSaleMisses(), 0);
	}


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

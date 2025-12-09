package com.acertainbookstore.business;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.acertainbookstore.interfaces.BookStore;
import com.acertainbookstore.interfaces.StockManager;
import com.acertainbookstore.utils.BookStoreConstants;
import com.acertainbookstore.utils.BookStoreException;
import com.acertainbookstore.utils.BookStoreUtility;

/** {@link TwoLevelLockingConcurrentCertainBookStore} implements the {@link BookStore} and
 * {@link StockManager} functionalities.
 * 
 * @see BookStore
 * @see StockManager
 */
public class TwoLevelLockingConcurrentCertainBookStore implements BookStore, StockManager {

	/** The mapping of books from ISBN to {@link BookStoreBook}. */
	private Map<Integer, BookStoreBook> bookMap = null;
	/** The global lock */
	private final ReadWriteLock globalLock = new ReentrantReadWriteLock();
	/** Lock on individual books (rows in the table, instances of object, whatever..) */
	private final Map<Integer, ReadWriteLock> lockMap = new HashMap<>();

	/**
	 * Instantiates a new {@link CertainBookStore}.
	 */
	public TwoLevelLockingConcurrentCertainBookStore() {
		// Constructors are not synchronized
		bookMap = new HashMap<>();
	}
	
	private void validate(StockBook book) throws BookStoreException {
		int isbn = book.getISBN();
		String bookTitle = book.getTitle();
		String bookAuthor = book.getAuthor();
		int noCopies = book.getNumCopies();
		float bookPrice = book.getPrice();

		if (BookStoreUtility.isInvalidISBN(isbn)) { // Check if the book has valid ISBN
			throw new BookStoreException(BookStoreConstants.ISBN + isbn + BookStoreConstants.INVALID);
		}

		if (BookStoreUtility.isEmpty(bookTitle)) { // Check if the book has valid title
			throw new BookStoreException(BookStoreConstants.BOOK + book.toString() + BookStoreConstants.INVALID);
		}

		if (BookStoreUtility.isEmpty(bookAuthor)) { // Check if the book has valid author
			throw new BookStoreException(BookStoreConstants.BOOK + book.toString() + BookStoreConstants.INVALID);
		}

		if (BookStoreUtility.isInvalidNoCopies(noCopies)) { // Check if the book has at least one copy
			throw new BookStoreException(BookStoreConstants.BOOK + book.toString() + BookStoreConstants.INVALID);
		}

		if (bookPrice < 0.0) { // Check if the price of the book is valid
			throw new BookStoreException(BookStoreConstants.BOOK + book.toString() + BookStoreConstants.INVALID);
		}

		if (bookMap.containsKey(isbn)) {// Check if the book is not in stock
			throw new BookStoreException(BookStoreConstants.ISBN + isbn + BookStoreConstants.DUPLICATED);
		}
	}	
	
	private void validate(BookCopy bookCopy) throws BookStoreException {
		int isbn = bookCopy.getISBN();
		int numCopies = bookCopy.getNumCopies();

		validateISBNInStock(isbn); // Check if the book has valid ISBN and in stock

		if (BookStoreUtility.isInvalidNoCopies(numCopies)) { // Check if the number of the book copy is larger than zero
			throw new BookStoreException(BookStoreConstants.NUM_COPIES + numCopies + BookStoreConstants.INVALID);
		}
	}
	
	private void validate(BookEditorPick editorPickArg) throws BookStoreException {
		int isbn = editorPickArg.getISBN();
		validateISBNInStock(isbn); // Check if the book has valid ISBN and in stock
	}
	
	private void validateISBNInStock(Integer ISBN) throws BookStoreException {
		if (BookStoreUtility.isInvalidISBN(ISBN)) { // Check if the book has valid ISBN
			throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.INVALID);
		}
		if (!bookMap.containsKey(ISBN)) {// Check if the book is in stock
			throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.NOT_AVAILABLE);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.acertainbookstore.interfaces.StockManager#addBooks(java.util.Set)
	 */
	public void addBooks(Set<StockBook> bookSet) throws BookStoreException {
		globalLock.writeLock().lock();
		try {
			if (bookSet == null) {
				throw new BookStoreException(BookStoreConstants.NULL_INPUT);
			}

			// Check if all are there
			for (StockBook book : bookSet) {
				validate(book);
			}

			for (StockBook book : bookSet) {
				int isbn = book.getISBN();
				bookMap.put(isbn, new BookStoreBook(book));
				lockMap.put(isbn, new ReentrantReadWriteLock());
			}
		} finally {
			globalLock.writeLock().unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.acertainbookstore.interfaces.StockManager#addCopies(java.util.Set)
	 */
	public void addCopies(Set<BookCopy> bookCopiesSet) throws BookStoreException {
		globalLock.readLock().lock();
		try {
			int isbn;
			int numCopies;

			if (bookCopiesSet == null) {
				throw new BookStoreException(BookStoreConstants.NULL_INPUT);
			}

			for (BookCopy bookCopy : bookCopiesSet) {
				validate(bookCopy);
			}

			BookStoreBook book;

			// Update the number of copies
			for (BookCopy bookCopy : bookCopiesSet) {
				isbn = bookCopy.getISBN();
				var bookLock = lockMap.get(isbn);
				try {
					// Lock book
					bookLock.writeLock().lock();
					numCopies = bookCopy.getNumCopies();
					book = bookMap.get(isbn);
					book.addCopies(numCopies);
				} finally { 
					bookLock.writeLock().unlock(); 
				}
			}
		} finally {
			globalLock.readLock().unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.StockManager#getBooks()
	 */
	public List<StockBook> getBooks() {
		globalLock.readLock().lock();
		try {
			Collection<BookStoreBook> bookMapValues = bookMap.values();
			return bookMapValues.stream().map(book -> {
				int isbn = book.getISBN();
				var bookLock = lockMap.get(isbn);
				bookLock.readLock().lock();
				try {
					return book.immutableStockBook();
				} finally { 
					bookLock.readLock().unlock(); 
				}
			})
			.collect(Collectors.toList());
		} finally { 
			globalLock.readLock().unlock(); 
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.acertainbookstore.interfaces.StockManager#updateEditorPicks(java.util
	 * .Set)
	 */
	public void updateEditorPicks(Set<BookEditorPick> editorPicks) throws BookStoreException {
		// Check that all ISBNs that we add/remove are there first.
		if (editorPicks == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}
		globalLock.readLock().lock();
		try {

			for (BookEditorPick editorPickArg : editorPicks) {
				validate(editorPickArg);
			}

			for (BookEditorPick editorPickArg : editorPicks) {
				var bookLock = lockMap.get(editorPickArg.getISBN());
				bookLock.writeLock().lock();
				try {
					bookMap.get(editorPickArg.getISBN()).setEditorPick(editorPickArg.isEditorPick());
				} finally {
					bookLock.writeLock().unlock();
				}
			}
		} finally { 
			globalLock.readLock().unlock(); 
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.BookStore#buyBooks(java.util.Set)
	 */
	public void buyBooks(Set<BookCopy> bookCopiesToBuy) throws BookStoreException {
		if (bookCopiesToBuy == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}
		// Simulate global intention
		globalLock.readLock().lock();
		try {
			// Check that all ISBNs that we buy are there first.
			int isbn;
			BookStoreBook book;
			Boolean saleMiss = false;

			Map<Integer, Integer> salesMisses = new HashMap<>();

			try {
				for (BookCopy bookCopyToBuy : bookCopiesToBuy) {
					isbn = bookCopyToBuy.getISBN();

					// Validate before locking, i.e. don't return null
					validate(bookCopyToBuy);
					var bookLock = lockMap.get(isbn);
					bookLock.writeLock().lock();

					book = bookMap.get(isbn);

					if (!book.areCopiesInStore(bookCopyToBuy.getNumCopies())) {
						// If we cannot sell the copies of the book, it is a miss.
						salesMisses.put(isbn, bookCopyToBuy.getNumCopies() - book.getNumCopies());
						saleMiss = true;
					}
				}

				// We throw exception now since we want to see how many books in the
				// order incurred misses which is used by books in demand
				if (saleMiss) {
					for (Map.Entry<Integer, Integer> saleMissEntry : salesMisses.entrySet()) {
						book = bookMap.get(saleMissEntry.getKey());
						book.addSaleMiss(saleMissEntry.getValue());
					}
					throw new BookStoreException(BookStoreConstants.BOOK + BookStoreConstants.NOT_AVAILABLE);
				}

				// Then make the purchase.
				for (BookCopy bookCopyToBuy : bookCopiesToBuy) {
					book = bookMap.get(bookCopyToBuy.getISBN());
					book.buyCopies(bookCopyToBuy.getNumCopies());
				}
			} finally {
				// Release all locks
				for (var bookCopy : bookCopiesToBuy) {
					var bookLock = lockMap.get(bookCopy.getISBN());
					bookLock.writeLock().unlock();
				}
			}
		} finally { 
			globalLock.readLock().unlock(); 
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.acertainbookstore.interfaces.StockManager#getBooksByISBN(java.util.
	 * Set)
	 */
	public List<StockBook> getBooksByISBN(Set<Integer> isbnSet) throws BookStoreException {
		if (isbnSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}
		globalLock.readLock().lock();
		try {
			for (Integer ISBN : isbnSet) {
				validateISBNInStock(ISBN);
			}
			return isbnSet.stream().map(isbn -> {
				var bookLock = lockMap.get(isbn);
				bookLock.readLock().lock();
				try {
					return bookMap.get(isbn).immutableStockBook();
				} finally { 
					bookLock.readLock().unlock();
				}
			})
			.collect(Collectors.toList());
		} finally { 
			globalLock.readLock().unlock(); 
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.BookStore#getBooks(java.util.Set)
	 */
	public List<Book> getBooks(Set<Integer> isbnSet) throws BookStoreException {
		if (isbnSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}
		globalLock.readLock().lock();
		try {
			// Lock all books first, then validate
			try {
				for (int isbn : isbnSet) {
					validateISBNInStock(isbn); // val before lock
					var bookLock = lockMap.get(isbn);
					bookLock.readLock().lock();
				}
				
				return isbnSet.stream()
						.map(isbn -> bookMap.get(isbn).immutableBook())
						.collect(Collectors.toList());
			} finally {
				// Release all locks
				for (int isbn : isbnSet) {
					var bookLock = lockMap.get(isbn);
					bookLock.readLock().unlock();
				}
			}
		} finally {
			globalLock.readLock().unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.BookStore#getEditorPicks(int)
	 */
	public List<Book> getEditorPicks(int numBooks) throws BookStoreException {
		if (numBooks < 0) {
			throw new BookStoreException("numBooks = " + numBooks + ", but it must be positive");
		}
		
		globalLock.readLock().lock();
		try {
			// Identify which books are editor picks (only need ISBNs)
			List<Integer> editorPickIsbns = bookMap.entrySet().stream()
				.map(Entry::getKey)  // Get ISBN
				.filter(isbn -> {
					var bookLock = lockMap.get(isbn);
					bookLock.readLock().lock();
					try {
						return bookMap.get(isbn).isEditorPick();
					} finally {
						bookLock.readLock().unlock();
					}
				})
				.collect(Collectors.toList());
			
			// Find numBooks random indices of books that will be picked
			Random rand = new Random();
			Set<Integer> tobePicked = new HashSet<>();
			int rangePicks = editorPickIsbns.size();
			
			if (rangePicks <= numBooks) {
				// We need to add all books
				for (int i = 0; i < editorPickIsbns.size(); i++) {
					tobePicked.add(i);
				}
			} else {
				// We need to pick randomly the books that need to be returned
				int randNum;
				while (tobePicked.size() < numBooks) {
					randNum = rand.nextInt(rangePicks);
					tobePicked.add(randNum);
				}
			}
			
			// Get the actual book data for selected indices
			return tobePicked.stream()
				.map(index -> {
					int isbn = editorPickIsbns.get(index);
					var bookLock = lockMap.get(isbn);
					bookLock.readLock().lock();
					try {
						return bookMap.get(isbn).immutableBook();
					} finally {
						bookLock.readLock().unlock();
					}
				})
				.collect(Collectors.toList());
				
		} finally { 
			globalLock.readLock().unlock(); 
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.BookStore#getTopRatedBooks(int)
	 */
	@Override
	public List<Book> getTopRatedBooks(int numBooks) throws BookStoreException {
		throw new BookStoreException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.StockManager#getBooksInDemand()
	 */
	@Override
	public List<StockBook> getBooksInDemand() throws BookStoreException {
		throw new BookStoreException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.BookStore#rateBooks(java.util.Set)
	 */
	@Override
	public void rateBooks(Set<BookRating> bookRating) throws BookStoreException {
		throw new BookStoreException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.acertainbookstore.interfaces.StockManager#removeAllBooks()
	 */
	public void removeAllBooks() throws BookStoreException {
		globalLock.writeLock().lock();
		try {
			bookMap.clear();
			lockMap.clear();
		} finally {
			globalLock.writeLock().unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.acertainbookstore.interfaces.StockManager#removeBooks(java.util.Set)
	 */
	public void removeBooks(Set<Integer> isbnSet) throws BookStoreException {
		if (isbnSet == null) {
			throw new BookStoreException(BookStoreConstants.NULL_INPUT);
		}

		globalLock.writeLock().lock();
		try {
			for (Integer ISBN : isbnSet) {
				if (BookStoreUtility.isInvalidISBN(ISBN)) {
					throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.INVALID);
				}

				if (!bookMap.containsKey(ISBN)) {
					throw new BookStoreException(BookStoreConstants.ISBN + ISBN + BookStoreConstants.NOT_AVAILABLE);
				}
			}

			for (int isbn : isbnSet) {
				bookMap.remove(isbn);
				lockMap.remove(isbn);
			} 
		} finally {
			globalLock.writeLock().unlock();
		}
	}
}

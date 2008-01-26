package org.adbcj;

/**
 * Receives notification of the logical results of a database query.  You may think of this as the SAX
 * @{link org.sax.ContentHandler} for database parsing.
 * 
 * <p>Each method accepts an accumulator that may be used for holding the parsing state.
 * 
 * @author Mike Heath
 *
 * @param <T>  The accumulator type.
 */
public interface ResultEventHandler<T> {

	/**
	 * Invoked when field definitions are about to be received.
	 * 
	 * @param accumulator
	 */
	void startFields(T accumulator);
	
	/**
	 * Invoked for each field definition.
	 * 
	 * @param field the field definition
	 * @param accumulator
	 */
	void field(Field field, T accumulator);
	
	/**
	 * Invoked when all field definitions have been received.
	 * 
	 * @param accumulator
	 */
	void endFields(T accumulator);
	
	/**
	 * Invoked when rest rows are about to be received.
	 * 
	 * @param accumulator
	 */
	void startResults(T accumulator);
	
	/**
	 * Invoked at the beginning of a data row.
	 * 
	 * @param accumulator
	 */
	void startRow(T accumulator);
	
	/**
	 * Invoked for each column in a data row.
	 * 
	 * @param value the value of the column 
	 * @param accumulator
	 */
	void value(Value value, T accumulator);
	
	/**
	 * Invoked at the end of a data row.
	 * 
	 * @param accumulator
	 */
	void endRow(T accumulator);
	
	/**
	 * Invoked after all the data rows have been processed.
	 * 
	 * @param accumulator
	 */
	void endResults(T accumulator);

	/**
	 * Invoked if there is an error processing the query.
	 * 
	 * @param t  the exception that was thrown
	 * @param accumulator
	 */
	void exception(Throwable t, T accumulator);
}

package emi.lib.mtg.v2;

/**
 * A set of card printings. This is meant to map cleanly onto i.e. "Unhinged".
 */
public interface Set {
	/**
	 * @return This set's name.
	 */
	String name();

	/**
	 * @return This set's code.
	 */
	String code();

	/**
	 * @return The set of cards printed in this set.
	 */
	java.util.Set<? extends Card.Printing> printings();
}

package emi.lib.mtg.v2;

public interface Set extends Card.Printing {
	String code();

	java.util.Set<? extends Card> cards();
}

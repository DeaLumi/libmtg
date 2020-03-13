package emi.lib.mtg.game;

import emi.lib.Service;
import emi.lib.mtg.Card;

import java.util.HashSet;
import java.util.Set;

@Service
@Service.Property.String(name="name")
public interface Format {
	enum Official {
		Freeform (null),
		Vintage (emi.lib.mtg.game.impl.formats.Vintage.class),
		Legacy (null),
		Modern (emi.lib.mtg.game.impl.formats.Modern.class),
		Frontier (null),
		Standard (emi.lib.mtg.game.impl.formats.Standard.class),

		Commander (emi.lib.mtg.game.impl.formats.EDH.class),
		Brawl (null);

		public final Class<? extends Format> formatClass;

		Official(Class<? extends Format> formatClass) {
			this.formatClass = formatClass;
		}
	}

	String name();

	Set<Zone> deckZones();

	boolean cardIsLegal(Card card);

	Set<String> validate(Deck deck);
}

package emi.lib.mtg.game.impl.formats;

import emi.lib.mtg.game.Deck;
import emi.lib.mtg.game.Zone;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public abstract class TraditionalFormat extends AbstractFormat {
	@Override
	public Set<Zone> deckZones() {
		return EnumSet.of(Zone.Library, Zone.Sideboard);
	}

	@Override
	public int minCards(Zone zone) {
		switch (zone) {
			case Hand:
			case Exile:
			case Stack:
			case Command:
			case Graveyard:
			case Battlefield:
				return 0;
			case Library:
				return 60;
			case Sideboard:
				return 0;
			default:
				assert false;
				return 0;
		}
	}

	@Override
	public int maxCards(Zone zone) {
		switch (zone) {
			case Hand:
			case Exile:
			case Stack:
			case Command:
			case Graveyard:
			case Battlefield:
				return 0;
			case Library:
				return -1;
			case Sideboard:
				return 15;
			default:
				assert false;
				return 0;
		}
	}

	@Override
	public int minCards() {
		return 60;
	}

	@Override
	public int maxCards() {
		return -1;
	}

	@Override
	public int maxCardCopies() {
		return 4;
	}

	@Override
	protected Set<String> furtherValidation(Deck deck) {
		return Collections.emptySet();
	}
}

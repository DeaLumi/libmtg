package emi.lib.mtg.game.impl.formats;

import emi.lib.Service;
import emi.lib.mtg.Card;
import emi.lib.mtg.game.Format;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service.Provider(Format.class)
@Service.Property.String(name="name", value="Standard")
public class Standard extends TraditionalFormat {

	private static final Set<String> BANLIST = banlist();

	private static Set<String> banlist() {
		return Collections.unmodifiableSet(Stream.of(
				"Aetherworks Marvel",
				"Smuggler's Copter",
				"Felidar Guardian",
				"Attune with Aether",
				"Rogue Refiner",
				"Rampaging Ferocidon",
				"Ramanup Ruins"
		).collect(Collectors.toSet()));
	}

	private static final Set<String> SETLIST = setlist();

	private static Set<String> setlist() {
		return Collections.unmodifiableSet(Stream.of(
				"Kaladesh",
				"Aether Revolt",
				"Amonkhet",
				"Welcome Deck 2017",
				"Hour of Devastation",
				"Ixalan",
				"Rivals of Ixalan",
				"Dominaria",
				"Core 2019"
		).collect(Collectors.toSet()));
	}

	@Override
	public String name() {
		return "Standard";
	}

	@Override
	public boolean setIsLegal(emi.lib.mtg.Set set) {
		return SETLIST.contains(set.name());
	}

	@Override
	public boolean cardIsBanned(Card card) {
		return BANLIST.contains(card.name());
	}
}

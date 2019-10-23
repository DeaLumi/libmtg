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
				"Field of the Dead"
		).collect(Collectors.toSet()));
	}

	private static final Set<String> SETLIST = setlist();

	private static Set<String> setlist() {
		return Collections.unmodifiableSet(Stream.of(
				"Guilds of Ravnica",
				"Ravnica Allegiance",
				"War of the Spark",
				"Core Set 2020",
				"Throne of Eldraine",
				"Theros: Beyond Death",
				"Ikoria: Lair of Behemoths"
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

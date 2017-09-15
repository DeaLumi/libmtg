package emi.lib.mtg.game.impl.formats;

import emi.lib.Service;
import emi.lib.mtg.Card;
import emi.lib.mtg.game.Format;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service.Provider(Format.class)
@Service.Property.String(name="name", value="Modern")
public class Modern extends TraditionalFormat {
	private static final Set<String> BANLIST = banlist();

	private static Set<String> banlist() {
		return Collections.unmodifiableSet(Stream.of(
				"Ancient Den",
				"Birthing Pod",
				"Blazing Shoal",
				"Bloodbraid Elf",
				"Chrome Mox",
				"Cloudpost",
				"Dark Depths",
				"Deathrite Shaman",
				"Dig Through Time",
				"Dread Return",
				"Eye of Ugin",
				"Gitaxian Probe",
				"Glimpse of Nature",
				"Golgari Grave-Troll",
				"Great Furnace",
				"Green Sun's Zenith",
				"Hypergenesis",
				"Jace, the Mind Sculptor",
				"Mental Misstep",
				"Ponder",
				"Preordain",
				"Punishing Fire",
				"Rite of Flame",
				"Seat of the Synod",
				"Second Sunrise",
				"Seething Song",
				"Sensei's Divining Top",
				"Skullclamp",
				"Splinter Twin",
				"Stoneforge Mystic",
				"Summer Bloom",
				"Treasure Cruise",
				"Tree of Tales",
				"Umezawa's Jitte",
				"Vault of Whispers"
		).collect(Collectors.toSet()));
	}

	private static final Set<String> SETLIST = setlist();

	// TODO: This is far from complete.
	private static Set<String> setlist() {
		return Collections.unmodifiableSet(Stream.of(
				"DOM",
				"RIX", "XLN",
				"HOU", "AKH",
				"AER", "KLD",
				"EMN", "SOI",
				"OGW", "BFZ",
				"ORI",
				"DTK", "FRF", "KTK",
				"JOU", "BNG", "THS",
				"DGM", "GTC", "RTR",
				"AVR", "DKA", "ISD",
				"NPH", "MBS", "SOM",
				"ROE", "WWK", "ZEN",
				"ARB", "CON", "ALA",
				"EVE", "SHM",
				"MOR", "LRW",
				"FUT", "PLC", "TSP",
				"DIS", "GPT", "RAV",
				"SOK", "BOK", "CHK",
				"5DN", "DST", "MRD",
				"M15", "M14", "M13", "M12", "M10", "10E", "9ED", "8ED",
				"CSP"
		).collect(Collectors.toSet()));
	}

	@Override
	public String name() {
		return "Modern";
	}

	@Override
	protected boolean setIsLegal(emi.lib.mtg.Set set) {
		return SETLIST.contains(set.code().toUpperCase());
	}

	@Override
	protected boolean cardIsBanned(Card card) {
		return BANLIST.contains(card.name());
	}
}

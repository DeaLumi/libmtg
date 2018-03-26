package emi.lib.mtg.game.impl.formats;

import emi.lib.Service;
import emi.lib.mtg.game.Format;
import emi.lib.mtg.Card;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service.Provider(Format.class)
@Service.Property.String(name="name", value="Vintage")
public class Vintage extends TraditionalFormat {
	public static final Set<String> BANLIST = banlist();

	private static Set<String> banlist() {
		return Collections.unmodifiableSet(Stream.of(
				// Cards with the card type Conspiracy
				"Adriana's Valor",
				"Advantageous Proclamation",
				"Assemble the Rank and Vile",
				"Backup Plan",
				"Brago's Favor",
				"Double Stroke",
				"Echoing Boon",
				"Emissary's Ploy",
				"Hired Heist",
				"Hold the Perimeter",
				"Hymn of the Wilds",
				"Immediate Action",
				"Incendiary Dissent",
				"Iterative Analysis",
				"Muzzio's Preparations",
				"Natural Unity",
				"Power Play",
				"Secret Summoning",
				"Secrets of Paradise",
				"Sentinel Dispatch",
				"Sovereign's Realm",
				"Summoner's Bond",
				"Unexpected Potential",
				"Weight Advantage",
				"Worldknit",

				// Cards that reference "playing for ante"
				"Amulet of Quoz",
				"Bronze Tablet",
				"Contract from Below",
				"Darkpact",
				"Demonic Attorney",
				"Jeweled Bird",
				"Rebirth",
				"Tempest Efreet",
				"Timmerian Fiends",

				// Other cards
				"Chaos Orb",
				"Falling Star",
				"Shahrazad"
		).collect(Collectors.toSet()));
	}

	@Override
	public String name() {
		return "Vintage";
	}

	@Override
	public boolean setIsLegal(emi.lib.mtg.Set set) {
		return true;
	}

	@Override
	public boolean cardIsBanned(Card card) {
		return BANLIST.contains(card.name());
	}
}

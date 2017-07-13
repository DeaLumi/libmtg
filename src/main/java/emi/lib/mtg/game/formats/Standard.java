package emi.lib.mtg.game.formats;

import emi.lib.Service;
import emi.lib.mtg.card.Card;
import emi.lib.mtg.characteristic.CardType;
import emi.lib.mtg.characteristic.Supertype;
import emi.lib.mtg.game.Deck;
import emi.lib.mtg.game.Format;
import emi.lib.mtg.game.Zone;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Service.Provider(Format.class)
@Service.Property.String(name="name", value="Standard")
public class Standard extends TraditionalFormat {
	@Override
	public String name() {
		return "Standard";
	}

}

package emi.lib.mtg.characteristic;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Created by Emi on 5/6/2016.
 */
public enum CardType {
	Artifact (true),
	Creature (true),
	Enchantment (true),
	Instant (false),
	Land (true),
	Planeswalker (true),
	Sorcery (false),
	Tribal (false),

	/*
	 * Types after this are not legal in usual constructed formats.
	 */

	Plane (true),
	Phenomena (true),
	Vanguard (true),
	Scheme (true),
	Conspiracy (true),
	Phenomenon (true),

	/*
	 * Types after this are weird types; they're used in 'silly' sets like Unhinged.
	 * (Technically 'Enchant' and 'Summon' are real (but old) types, but data for
	 * modern sets is oracle-ized, so these types now only appear in un-sets.)
	 */

	Eaturecray (false, true),
	Enchant (false, true),
	Summon (false, true),
	Player (false, true),
	Host (false, true),
	Hero (false, true),
	Elemental (false, true),
	Autobot (false, true),
	Character (false, true);

	public final boolean constructed;
	public final boolean permanent;

	CardType(boolean permanent) {
		this(true, permanent);
	}

	CardType(boolean constructed, boolean permanent) {
		this.constructed = constructed;
		this.permanent = permanent;
	}
}

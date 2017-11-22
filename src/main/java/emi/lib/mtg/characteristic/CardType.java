package emi.lib.mtg.characteristic;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Created by Emi on 5/6/2016.
 */
public enum CardType {
	Artifact,
	Creature,
	Enchantment,
	Instant,
	Land,
	Planeswalker,
	Sorcery,
	Tribal,

	/*
	 * Types after this are not legal in usual constructed formats.
	 */

	Plane,
	Phenomena,
	Vanguard,
	Scheme,
	Conspiracy,
	Phenomenon,

	/*
	 * Types after this are weird types; they're used in 'silly' sets like Unhinged.
	 * (Technically 'Enchant' and 'Summon' are real (but old) types, but data for
	 * modern sets is oracle-ized, so these types now only appear in un-sets.)
	 */

	Eaturecray,
	Enchant,
	Summon,
	Player,
	Host;

	public static final Set<CardType> CONSTRUCTED_TYPES = Collections.unmodifiableSet(EnumSet.of(
			CardType.Artifact,
			CardType.Creature,
			CardType.Enchantment,
			CardType.Instant,
			CardType.Land,
			CardType.Planeswalker,
			CardType.Sorcery,
			CardType.Tribal
	));
}

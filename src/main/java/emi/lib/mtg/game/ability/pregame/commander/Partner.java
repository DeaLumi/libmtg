package emi.lib.mtg.game.ability.pregame.commander;

import emi.lib.mtg.Card;
import emi.lib.mtg.TypeLine;
import emi.lib.mtg.enums.CardType;
import emi.lib.mtg.enums.Supertype;
import emi.lib.mtg.game.Deck;
import emi.lib.mtg.game.Format;
import emi.lib.mtg.game.ability.Ability;
import emi.lib.mtg.game.validation.Commander;

import java.util.Collection;
import java.util.regex.Matcher;

public interface Partner extends CommandZoneOverride {
	@Override
	default Class<? extends CommandZoneOverride> constraintFamily() {
		return Partner.class;
	}

	abstract class PartnerGroup implements Partner {
		public static class StandardPartner extends PartnerGroup {
			public static class Parser implements Ability.Parser {
				@Override
				public Class<? extends Ability> type() {
					return StandardPartner.class;
				}

				@Override
				public String pattern() {
					return "Partner(?: \\([^)]+\\))?";
				}

				@Override
				public Ability make(Card.Face card, Matcher match) {
					return new StandardPartner();
				}
			}

			@Override
			public String text() {
				return "Partner";
			}
		}

		public static class FriendsForever extends PartnerGroup {
			public static class Parser implements Ability.Parser {
				@Override
				public Class<? extends Ability> type() {
					return FriendsForever.class;
				}

				@Override
				public String pattern() {
					return "Friends forever(?: \\([^)]+\\))?";
				}

				@Override
				public Ability make(Card.Face card, Matcher match) {
					return new FriendsForever();
				}
			}

			@Override
			public String text() {
				return "Friends forever";
			}
		}

		@Override
		public Format.Validator.Result check(Card.Printing source, Deck deck, Collection<? extends Card.Printing> commanders) {
			// CR702.124h: "Partner" means "You may designate two legendary creature cards as your commander rather than
			//    one if each of them has partner.
			// CR702.124j: "Friends forever" means "You may designate two legendary creature cards as your commander
			//    rather than one if each of them has friends forever."
			// My interpretation:
			// 1. A card with Partner or Friends forever is legal if it is a valid commander and either:
			//    a. It is the only commander, or
			//    b. It is paired with exactly one other card which:
			//       i. Is a valid commander, and
			//       ii. Has Partner or Friends forever, whichever this card has.

			Format.Validator.Result result = new Format.Validator.Result();

			// A card with Partner is legal if it is a valid commander and...
			if (!Commander.validateCommander(source, deck, result)) return result;

			// ...it is either the only commander, or...
			if (commanders.size() == 1) return result;

			// ...it is paired with exactly one other card which...
			Card.Printing partner = commanders.stream().filter(pr -> pr != source).reduce(null, (a, b) -> (a != null) ^ (b != null) ? (a != null ? a : b) : null);

			if (partner == null) {
				result.card(source).errors.add(String.format("%s can only be partnered with exactly one other valid commander with %s.", source.card().name(), text()));
				return result;
			}

			// ...i. Is a valid commander and...
			if (!Commander.validateCommander(partner, deck, result)) return result;

			// ...ii. Has Partner.
			Partner partnersPartner = partner.card().front() != null ? partner.card().front().abilities().only(this.getClass()) : null;
			if (partnersPartner == null) {
				result.card(source).errors.add(String.format("%s's partner must have %s.", source.card().name(), text()));
				return result;
			}

			return Format.Validator.Result.EMPTY;
		}
	}

	class PartnerWith implements Partner {
		public static class Parser implements Ability.Parser {
			@Override
			public Class<? extends Ability> type() {
				return PartnerWith.class;
			}

			@Override
			public String pattern() {
				return "Partner with (?<with>" + CARD_NAME + ")(?: \\([^)]+\\))?";
			}

			@Override
			public Ability make(Card.Face card, Matcher match) {
				return new PartnerWith(match.group("with"));
			}
		}

		public final String with;

		public PartnerWith(String with) {
			this.with = with;
		}

		@Override
		public String text() {
			return String.format("Partner with %s", with);
		}

		@Override
		public Format.Validator.Result check(Card.Printing source, Deck deck, Collection<? extends Card.Printing> commanders) {
			// CR702.124i: "Partner with [name]" represents two abilities. It means "You may designate two legendary
			//    creature cards as your commander rather than one if each has a 'partner with [name]' ability with the
			//    other's name" and "When this permanent enters the battlefield, target player may search their library
			//    for a card named [name], reveal it, put it into their hand, then shuffle."
			// My interpretation:
			// 1. A card with "Partner with [name]" is legal if it is a valid commander and:
			//    a. It is the only commander, or
			//    b. It is paired with exactly one other card which:
			//       i. Is a valid commander, and
			//       ii. Is named "[name]", and
			//       iii. Has "Partner with [name]" referencing this card's name.

			Format.Validator.Result result = new Format.Validator.Result();

			// A card with "Partner with [name]" is legal if it is a valid commander and...
			if (!Commander.validateCommander(source, deck, result)) return result;

			// ...it is either the only commander, or...
			if (commanders.size() == 1) return result;

			// ...it is paired with exactly one other card which...
			Card.Printing partner = commanders.stream().filter(pr -> pr != source).reduce(null, (a, b) -> (a != null) ^ (b != null) ? (a != null ? a : b) : null);

			if (partner == null) {
				result.card(source).errors.add(String.format("%s can only be partnered with exactly one other valid commander named %s.", source.card().name(), with));
				return result;
			}

			// ...i. Is a valid commander, and...
			if (!Commander.validateCommander(partner, deck, result)) return result;

			// ...ii. Is named "[name]", and...
			if (!with.equals(partner.card().name())) {
				result.card(source).errors.add(String.format("%s can only be partnered with exactly one other valid commander named %s.", source.card().name(), with));
				return result;
			}

			// ...ii. Has "Partner with [name]" referencing this card's name.
			PartnerWith partnersWith = partner.card().front() != null ? partner.card().front().abilities().only(PartnerWith.class) : null;

			if (partnersWith == null || !partnersWith.with.equals(source.card().name())) {
				result.card(source).errors.add(String.format("%1$s's partner must have \"Partner with %1$s\".", source.card().name()));
				return result;
			}

			return Format.Validator.Result.EMPTY;
		}
	}

	class LegendaryPartner implements Partner {
		public static class Parser implements Ability.Parser {
			@Override
			public Class<? extends Ability> type() {
				return LegendaryPartner.class;
			}

			@Override
			public String pattern() {
				return "Legendary partner(?: \\([^)]+\\))?";
			}

			@Override
			public Ability make(Card.Face card, Matcher match) {
				return new LegendaryPartner();
			}
		}

		@Override
		public String text() {
			return "Legendary partner";
		}

		@Override
		public Format.Validator.Result check(Card.Printing source, Deck deck, Collection<? extends Card.Printing> commanders) {
			// There is no comprehensive rules reference for Legendary partner. It is only found on the Heroes of the
			// Realm card Sol, Advocate Eternal. Its reminder text is as follows:
			//    "You can have two commanders if this is one of them. The other one is promoted to legendary."
			// My interpretation:
			// 1. A card with Legendary partner is legal if it is a valid commander and:
			//    a. It is the only commander, or
			//    b. It is paired with exactly one other card which:
			//       i. Is a creature card.

			Format.Validator.Result result = new Format.Validator.Result();

			// A card with Legendary partner is legal if it is a valid commander and...
			if (!Commander.validateCommander(source, deck, result)) return result;

			// ...it is either the only commander, or...
			if (commanders.size() == 1) return result;

			// ...it is paired with exactly one other card which...
			// ...i. Is a creature card.
			Card.Printing partner = commanders.stream().filter(pr -> pr != source).reduce(null, (a, b) -> (a != null) ^ (b != null) ? (a != null ? a : b) : null);

			if (partner == null || partner.card().front() == null || !partner.card().front().type().is(CardType.Creature)) {
				result.card(source).errors.add(String.format("%s can only be partnered with exactly one other creature card.", source.card().name()));
				return result;
			}

			return Format.Validator.Result.EMPTY;
		}
	}

	class ChooseABackground implements Partner {
		public static class Parser implements Ability.Parser {
			@Override
			public Class<? extends Ability> type() {
				return ChooseABackground.class;
			}

			@Override
			public String pattern() {
				return "Choose a Background(?: \\([^)]+\\))?";
			}

			@Override
			public Ability make(Card.Face card, Matcher match) {
				return new ChooseABackground();
			}
		}

		@Override
		public String text() {
			return "Choose a Background";
		}

		@Override
		public Format.Validator.Result check(Card.Printing source, Deck deck, Collection<? extends Card.Printing> commanders) {
			// CR702.124k: "Choose a Background" means "You may designate two cards as your commander rather than one if
			//    one of them is this card and the other is a legendary Background enchantment card." You can't
			//    designate two cards as your commander if one has a "choose a Background" ability and the other is not
			//    a legendary Background enchantment card, and legendary Background enchantment cards can't be your
			//    commander unless you have also designated a commander with "choose a Background."
			// My interpretation:
			// 1. A card with "Choose a Background" is legal if it is a valid commander and:
			//    a. It is the only commander, or
			//    b. It is paired with exactly one other card which:
			//       i. Is a legendary Enchantment background.
			// (Note that the comp rules technically forbid a card with both Partner and Choose a background from having
			// a partner. I think that's likely a mistaken restriction, so I've relaxed it.)

			Format.Validator.Result result = new Format.Validator.Result();

			// A card with Choose a Background is legal if it is a valid commander and...
			if (!Commander.validateCommander(source, deck, result)) return result;

			// ...it is either the only commander, or...
			if (commanders.size() == 1) return result;

			// ...it is paired with exactly one other card which...
			// ...i. is a legendary Enchantment background.
			Card.Printing partner = commanders.stream().filter(pr -> pr != source).reduce(null, (a, b) -> (a != null) ^ (b != null) ? (a != null ? a : b) : null);
			TypeLine partnerType = partner != null && partner.card().front() != null ? partner.card().front().type() : null;

			if (partnerType == null || !partnerType.is(Supertype.Legendary) || !partnerType.is(CardType.Enchantment) || !partnerType.is("Background")) {
				result.card(source).errors.add(String.format("%s can only be partnered with a legendary Enchantment background.", source.card().name()));
				return result;
			}

			return Format.Validator.Result.EMPTY;
		}
	}

	class DoctorsCompanion implements Partner {
		public static class Parser implements Ability.Parser {
			@Override
			public Class<? extends Ability> type() {
				return DoctorsCompanion.class;
			}

			@Override
			public String pattern() {
				return "Doctor's companion(?: \\([^)]+\\))?";
			}

			@Override
			public Ability make(Card.Face card, Matcher match) {
				return new DoctorsCompanion();
			}
		}

		@Override
		public String text() {
			return "Doctor's companion";
		}

		@Override
		public Format.Validator.Result check(Card.Printing source, Deck deck, Collection<? extends Card.Printing> commanders) {
			// CR702.124m. "Doctor's companion" means "You may designate two legendary creature cards as your commander
			//    rather than one if one of them is this card and the other is a legendary Time Lord Doctor creature
			//    card that has no other creature types."
			// My interpretation:
			// 1. A card with "Doctor's companion" is legal if it is a valid commander and:
			//    a. It is the only commander, or
			//    b. It is paired with exactly one other card which:
			//       i. Is a legendary creature, and
			//       ii. has exactly the subtypes Time Lord and Doctor.

			Format.Validator.Result result = new Format.Validator.Result();

			// A card with Doctor's companion is legal if it is a valid commander and...
			if (!Commander.validateCommander(source, deck, result)) return result;

			// ...it is either the only commander, or...
			if (commanders.size() == 1) return result;

			// ...it is paired with exactly one other card which...
			// ...i. is a legendary creature, and
			// ...ii. has exactly the subtypes Time Lord and Doctor.
			Card.Printing partner = commanders.stream().filter(pr -> pr != source).reduce(null, (a, b) -> (a != null) ^ (b != null) ? (a != null ? a : b) : null);
			TypeLine partnerType = partner != null && partner.card().front() != null ? partner.card().front().type() : null;

			// TODO: Time Lord is a singular creature type.
			if (partnerType == null
					|| !partnerType.is(Supertype.Legendary) || !partnerType.is(CardType.Creature)
					|| !partnerType.is("Time") || !partnerType.is("Lord") || !partnerType.is("Doctor") || partnerType.subtypes().size() != 3) {
				result.card(source).errors.add(String.format("%s can only be partnered with a legendary Time Lord Doctor creature with no other creature types.", source.card().name()));
				return result;
			}

			return Format.Validator.Result.EMPTY;
		}
	}

	class CreateACharacter implements Partner {
		public static class Parser implements Ability.Parser {
			@Override
			public Class<? extends Ability> type() {
				return CreateACharacter.class;
			}

			@Override
			public String pattern() {
				return "Create a Character(?: \\([^)]+\\))?";
			}

			@Override
			public Ability make(Card.Face card, Matcher match) {
				return new CreateACharacter();
			}
		}

		@Override
		public String text() {
			return "Create a Character";
		}

		@Override
		public Format.Validator.Result check(Card.Printing source, Deck deck, Collection<? extends Card.Printing> commanders) {
			// There is no comprehensive rules reference for Create a Character. It is only found on the Heroes of the
			// Realm card Wizard from Beyond. Its reminder text is as follows:
			//    "Any nonlegendary creature can choose this as its Background. It becomes legendary and can be your
			//    commander."
			// My interpretation:
			// 1. A card with Create a Character is legal if:
			//    a. It is a legendary Enchantment background, and
			//    b. It is paired with exactly one other card which:
			//       i. Is a nonlegendary creature card.

			Format.Validator.Result result = new Format.Validator.Result();

			// A card with Create a Character is legal if it is a legendary Enchantment background and...
			TypeLine sourceType = source.card().front() != null ? source.card().front().type() : null;
			if (sourceType == null || !sourceType.is(Supertype.Legendary) || !sourceType.is(CardType.Enchantment) || !sourceType.is("Background")) {
				result.card(source).errors.add(String.format("%s must be a legendary Enchantment background.", source.card().name()));
				return result;
			}

			// ...b. it is paired with exactly one other card which...
			// ...i. is a nonlegendary creature card.
			Card.Printing partner = commanders.stream().filter(pr -> pr != source).reduce(null, (a, b) -> (a != null) ^ (b != null) ? (a != null ? a : b) : null);
			TypeLine partnerType = partner != null && partner.card().front() != null ? partner.card().front().type() : null;

			if (partnerType == null || partnerType.is(Supertype.Legendary) || !partnerType.is(CardType.Creature)) {
				result.card(source).errors.add(String.format("%s must be paired with exactly one nonlegendary creature.", source.card().name()));
				return result;
			}

			return Format.Validator.Result.EMPTY;
		}
	}


}

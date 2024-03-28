package emi.lib.mtg.game.ability.pregame.commander;

import emi.lib.mtg.Card;
import emi.lib.mtg.game.Format;
import emi.lib.mtg.game.ability.Ability;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.stream.Stream;

public class PartnerCommander implements CommanderOverride {
	public static class Parser implements Ability.Parser {
		@Override
		public Class<PartnerCommander> type() {
			return PartnerCommander.class;
		}

		@Override
		public String pattern() {
			return "Friends forever|(?<partnerLegendary>[Ll]egendary )?[Pp]artner(?: with (?<partnerWith>" + CARD_NAME + "))?(?: \\([^)]+\\))?";
		}

		@Override
		public PartnerCommander make(Card.Face face, Matcher match) {
			// TODO This should also return the PartnerWithEtb if appropriate.
			return new PartnerCommander(match.group("partnerWith"), match.group("partnerLegendary") != null);
		}
	}

	public final String with;
	public final boolean legendary;

	public PartnerCommander(String with, boolean legendary) {
		this.with = with;
		this.legendary = legendary;
	}

	public boolean check(Card.Printing source, Collection<? extends Card.Printing> commanders, Format.ValidationResult result) {
		Stream<? extends Card.Printing> partners = commanders.stream()
				.filter(pr -> pr != source);

		if (with != null) {
			partners = partners.filter(pr -> with.equals(pr.card().name()));
		} else if (!legendary) {
			partners = partners.filter(pr -> {
				Card.Face front = pr.card().front();
				if (front == null) return false;
				PartnerCommander partner = front.abilities().only(PartnerCommander.class);
				return partner != null && partner.with == null;
			});
		}

		long partnerCount = partners.count();
		if (commanders.size() <= 2 && commanders.size() == partnerCount + 1) return true;

		if (with != null) {
			result.card(source).errors.add(String.format("%s can only be paired with %s", source.card().name(), with));
		} else {
			result.card(source).errors.add(String.format("%s can only be paired with one other card with partner.", source.card().name()));
		}

		return false;
	}

	@Override
	public String text() {
		return (legendary ? "Legendary partner" : "Partner") + (with == null ? "" : " with " + with);
	}
}

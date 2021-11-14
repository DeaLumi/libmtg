package emi.lib.mtg.game.ability;

import emi.lib.mtg.Card;
import emi.lib.mtg.Mana;
import emi.lib.mtg.TypeLine;
import emi.lib.mtg.characteristic.Color;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Abilities {
	Collection<Ability> allAbilities();

	default <T extends Ability> Stream<T> ofType(Class<T> type) {
		return allAbilities().stream()
				.filter(s -> type.isAssignableFrom(s.getClass()))
				.map(type::cast);
	}

	default <T extends Ability> T only(Class<T> type) {
		List<T> abilities = ofType(type).collect(Collectors.toList());

		if (abilities.size() > 1) throw new IllegalStateException("A card shouldn't have more than one " + type.getSimpleName() + " ability!");
		if (abilities.size() < 1) return null;
		return abilities.get(0);
	}

	class DefaultAbilities implements Abilities {
		private static final ServiceLoader<Ability.Parser> PARSERS = ServiceLoader.load(Ability.Parser.class);
		private static final Pattern METAPATTERN;
		private static final Map<String, Ability.Parser> PARSER_MAP;

		static {
			boolean first = true;
			StringBuilder patternBuilder = new StringBuilder("(?m)^(?:");
			Map<String, Ability.Parser> parserMap = new HashMap<>();
			for (Ability.Parser parser : PARSERS) {
				if (first) {
					first = false;
				} else {
					patternBuilder.append('|');
				}

				patternBuilder.append("(?<").append(parser.type().getSimpleName()).append('>').append(parser.pattern()).append(')');
				parserMap.put(parser.type().getSimpleName(), parser);
			}
			patternBuilder.append(")$");
			METAPATTERN = Pattern.compile(patternBuilder.toString());
			PARSER_MAP = Collections.unmodifiableMap(parserMap);
		}

		private final Set<Ability> backing;

		public DefaultAbilities(Card.Face face) {
			Set<Ability> backing = new HashSet<>();

			Matcher matcher = METAPATTERN.matcher(face.rules());
			while (matcher.find()) {
				// I hate having to iterate here. Why can't a matcher tell me the list of group names it matched?
				for (Map.Entry<String, Ability.Parser> parser : PARSER_MAP.entrySet()) {
					if (matcher.group(parser.getKey()) != null) {
						backing.add(parser.getValue().make(face, matcher));
					}
				}
			}

			this.backing = backing;
		}

		@Override
		public Collection<Ability> allAbilities() {
			return backing;
		}
	}
}

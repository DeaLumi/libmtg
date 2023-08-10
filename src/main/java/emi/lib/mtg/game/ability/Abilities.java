package emi.lib.mtg.game.ability;

import emi.lib.mtg.Card;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public interface Abilities {
	Collection<Ability> allAbilities();

	default Stream<Ability> stream() {
		return allAbilities().stream();
	}

	default <T extends Ability> Stream<T> ofType(Class<T> type) {
		return allAbilities().stream()
				.filter(s -> type.isAssignableFrom(s.getClass()))
				.map(type::cast);
	}

	default <T extends Ability> T only(Class<T> type) {
		return ofType(type).reduce(null, (a, b) -> (a != null ^ b != null) ? (a != null ? a : b) : null);
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

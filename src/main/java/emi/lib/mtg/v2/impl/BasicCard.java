package emi.lib.mtg.v2.impl;

import com.google.common.collect.EnumHashBiMap;
import emi.lib.mtg.characteristic.CardTypeLine;
import emi.lib.mtg.characteristic.Color;
import emi.lib.mtg.characteristic.ManaCost;
import emi.lib.mtg.characteristic.impl.BasicCardTypeLine;
import emi.lib.mtg.characteristic.impl.BasicManaCost;
import emi.lib.mtg.v2.Card;

import java.util.*;

@SuppressWarnings({"unused", "WeakerAccess"})
public class BasicCard implements Card {
	public class Face implements Card.Face {
		private Kind kind;
		private String name;
		private ManaCost manaCost;
		private Set<Color> colorIndicator;
		private CardTypeLine type;
		private String rules;
		private String power;
		private String toughness;
		private String loyalty;
		private String handModifier;
		private String lifeModifier;

		public Face() {
			this.kind = Kind.Front;
			this.name = "";
			this.manaCost = BasicManaCost.parse("");
			this.colorIndicator = EnumSet.noneOf(Color.class);
			this.type = BasicCardTypeLine.parse("");
			this.rules = "";
			this.power = "";
			this.toughness = "";
			this.loyalty = "";
			this.handModifier = "";
			this.lifeModifier = "";
		}

		@Override
		public Card card() {
			return BasicCard.this;
		}

		@Override
		public Kind kind() {
			return this.kind;
		}

		public Face kind(Kind kind) {
			this.kind = kind;
			return this;
		}

		@Override
		public String name() {
			return this.name;
		}

		public Face name(String name) {
			this.name = name;
			return this;
		}

		@Override
		public ManaCost manaCost() {
			return this.manaCost;
		}

		public Face manaCost(ManaCost manaCost) {
			this.manaCost = manaCost;
			return this;
		}

		public Face manaCost(String manaCost) {
			return this.manaCost(BasicManaCost.parse(manaCost));
		}

		@Override
		public Set<Color> colorIndicator() {
			return this.colorIndicator;
		}

		public Face colorIndicator(Collection<Color> colors) {
			this.colorIndicator = EnumSet.copyOf(colors);
			return this;
		}

		public Face colorIndicator(Color... colors) {
			return this.colorIndicator(Arrays.asList(colors));
		}

		@Override
		public CardTypeLine type() {
			return this.type;
		}

		public Face type(CardTypeLine type) {
			this.type = type;
			return this;
		}

		public Face type(String type) {
			this.type = BasicCardTypeLine.parse(type);
			return this;
		}

		@Override
		public String rules() {
			return this.rules;
		}

		public Face rules(String rules) {
			this.rules = rules;
			return this;
		}

		@Override
		public String power() {
			return this.power;
		}

		public Face power(String power) {
			this.power = power;
			return this;
		}

		public Face power(double power) {
			return this.power(Double.toString(power));
		}

		@Override
		public String toughness() {
			return this.toughness;
		}

		public Face toughness(String toughness) {
			this.toughness = toughness;
			return this;
		}

		public Face toughness(double toughness) {
			return this.toughness(Double.toString(toughness));
		}

		@Override
		public String loyalty() {
			return this.loyalty;
		}

		public Face loyalty(String loyalty) {
			this.loyalty = loyalty;
			return this;
		}

		public Face loyalty(double loyalty) {
			return this.loyalty(Double.toString(loyalty));
		}

		@Override
		public String handModifier() {
			return this.handModifier;
		}

		public Face handModifier(String handModifier) {
			this.handModifier = handModifier;
			return this;
		}

		public Face handModifier(double handModifier) {
			return this.handModifier(Double.toString(handModifier));
		}

		@Override
		public String lifeModifier() {
			return this.lifeModifier;
		}

		public Face lifeModifier(String lifeModifier) {
			this.lifeModifier = lifeModifier;
			return this;
		}

		public Face lifeModifier(double lifeModifier) {
			return this.lifeModifier(Double.toString(lifeModifier));
		}
	}

	private EnumHashBiMap<Face.Kind, Card.Face> faces;
	private Set<Card.Printing> printings;
	private UUID id;

	public BasicCard() {
		this.faces = EnumHashBiMap.create(Face.Kind.class);
		this.printings = new HashSet<>();
		this.id = UUID.randomUUID();
	}

	@Override
	public Set<Card.Face> faces() {
		return this.faces.values();
	}

	@Override
	public Card.Face face(Face.Kind kind) {
		return this.faces.get(kind);
	}

	public BasicCard face(Face.Kind kind, Card.Face face) {
		this.faces.put(kind, face);
		return this;
	}

	@Override
	public Set<Card.Printing> printings() {
		return this.printings;
	}
}
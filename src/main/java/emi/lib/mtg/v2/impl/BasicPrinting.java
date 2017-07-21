package emi.lib.mtg.v2.impl;

import emi.lib.mtg.characteristic.CardRarity;
import emi.lib.mtg.v2.Card;
import emi.lib.mtg.v2.Set;

import java.util.UUID;

@SuppressWarnings("unused")
public class BasicPrinting implements Card.Printing {
	private Card card;
	private Set set;
	private CardRarity rarity;
	private Integer multiverseId;
	private int variation;
	private String collectorNumber;
	private Integer mtgoCatalogId;
	private UUID id;

	public BasicPrinting() {
		this.card = null;
		this.set = null;
		this.rarity = CardRarity.Common;
		this.multiverseId = null;
		this.variation = 1;
		this.collectorNumber = null;
		this.mtgoCatalogId = null;
		this.id = UUID.randomUUID();
	}

	@Override
	public Card card() {
		return this.card;
	}

	public BasicPrinting card(Card card) {
		this.card = card;
		return this;
	}

	@Override
	public Set set() {
		return this.set;
	}

	public BasicPrinting set(Set set) {
		this.set = set;
		return this;
	}

	@Override
	public CardRarity rarity() {
		return this.rarity;
	}

	public BasicPrinting rarity(CardRarity rarity) {
		this.rarity = rarity;
		return this;
	}

	@Override
	public Integer multiverseId() {
		return this.multiverseId;
	}

	public BasicPrinting multiverseId(Integer mvid) {
		this.multiverseId = mvid;
		return this;
	}

	@Override
	public int variation() {
		return this.variation;
	}

	public BasicPrinting variation(int variation) {
		this.variation = variation;
		return this;
	}

	@Override
	public String collectorNumber() {
		return this.collectorNumber;
	}

	public BasicPrinting collectorNumber(String collectorNumber) {
		this.collectorNumber = collectorNumber;
		return this;
	}

	@Override
	public Integer mtgoCatalogId() {
		return this.mtgoCatalogId;
	}

	public BasicPrinting mtgoCatalogId(Integer mtgoCatalogId) {
		this.mtgoCatalogId = mtgoCatalogId;
		return this;
	}

	@Override
	public UUID id() {
		return this.id;
	}

	public BasicPrinting id(UUID id) {
		this.id = id;
		return this;
	}
}

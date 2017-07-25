package emi.lib.mtg.impl;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import emi.lib.mtg.Card;
import emi.lib.mtg.Set;

import java.util.UUID;

@SuppressWarnings("unused")
public class BasicSet implements Set {
	private String name;
	private String code;
	private BiMap<UUID, ? extends Card.Printing> printings;

	public BasicSet() {
		this.name = "";
		this.code = "";
		this.printings = HashBiMap.create();
	}

	@Override
	public String name() {
		return this.name;
	}

	public BasicSet name(String name) {
		this.name = name;
		return this;
	}

	@Override
	public String code() {
		return this.code;
	}

	public BasicSet code(String code) {
		this.code = code;
		return this;
	}

	@Override
	public java.util.Set<? extends Card.Printing> printings() {
		return this.printings.values();
	}

	@Override
	public Card.Printing printing(UUID id) {
		return this.printings.get(id);
	}
}

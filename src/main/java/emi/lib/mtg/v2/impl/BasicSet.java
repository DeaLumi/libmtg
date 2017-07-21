package emi.lib.mtg.v2.impl;

import emi.lib.mtg.v2.Card;
import emi.lib.mtg.v2.Set;

import java.util.HashSet;

public class BasicSet implements Set {
	private String name;
	private String code;
	private java.util.Set<? extends Card.Printing> printings;

	public BasicSet() {
		this.name = "";
		this.code = "";
		this.printings = new HashSet<>();
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
		return this.printings;
	}
}

package emi.lib.mtg.game;

import emi.lib.Service;

import java.util.Set;

@Service
@Service.Property.String(name="name")
public interface Format {
	String name();

	Set<Zone> deckZones();

	Set<String> validate(Deck deck);
}

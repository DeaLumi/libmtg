package emi.lib.mtg.game;

public enum Zone {
	Library (Type.Private),
	Hand (Type.Private),
	Battlefield (Type.Shared),
	Graveyard (Type.Public),
	Stack (Type.Shared),
	Exile (Type.Shared),
	Command (Type.Shared),
	Sideboard (Type.NonGame);

	public enum Type {
		NonGame,
		Private,
		Public,
		Shared
	}

	public final Type type;

	Zone(Type type) {
		this.type = type;
	}
}

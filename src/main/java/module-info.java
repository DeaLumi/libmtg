module emi.lib.mtg {
	requires java.desktop;

	exports emi.lib.mtg;
	exports emi.lib.mtg.enums;
	exports emi.lib.mtg.game;
	exports emi.lib.mtg.game.ability;
	exports emi.lib.mtg.game.ability.pregame;
	exports emi.lib.mtg.game.validation;
	exports emi.lib.mtg.img;
	exports emi.lib.mtg.util;

	uses emi.lib.mtg.game.ability.Ability.Parser;
	provides emi.lib.mtg.game.ability.Ability.Parser with
		emi.lib.mtg.game.ability.pregame.commander.CommanderOverride$CanBeCommander.Parser,
		emi.lib.mtg.game.ability.pregame.Companion.Parser,
		emi.lib.mtg.game.ability.pregame.CopyLimit.Parser,
		emi.lib.mtg.game.ability.pregame.commander.Partner.PartnerGroup.StandardPartner.Parser,
		emi.lib.mtg.game.ability.pregame.commander.Partner.PartnerGroup.FriendsForever.Parser,
		emi.lib.mtg.game.ability.pregame.commander.Partner.PartnerWith.Parser,
		emi.lib.mtg.game.ability.pregame.commander.Partner.LegendaryPartner.Parser,
		emi.lib.mtg.game.ability.pregame.commander.Partner.ChooseABackground.Parser,
		emi.lib.mtg.game.ability.pregame.commander.Partner.DoctorsCompanion.Parser,
		emi.lib.mtg.game.ability.pregame.commander.Partner.CreateACharacter.Parser;
}

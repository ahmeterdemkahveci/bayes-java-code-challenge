package gg.bayes.challenge.business.service.match;

import gg.bayes.challenge.config.rest.model.HeroDamage;
import gg.bayes.challenge.config.rest.model.HeroItem;
import gg.bayes.challenge.config.rest.model.HeroKills;
import gg.bayes.challenge.config.rest.model.HeroSpells;

import java.util.List;

public interface MatchLogQueryService {

	List<HeroKills> getHeroKillsByMatch(Long matchId);

	List<HeroSpells> getHeroSpellsByMatchAndHero(Long matchId, String heroName);

	List<HeroDamage> getHeroDamageByMatch(Long matchId, String heroName);

	List<HeroItem> getHeroItemsByMatch(Long matchId, String heroName);
}

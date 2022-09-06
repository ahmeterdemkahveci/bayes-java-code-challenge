package gg.bayes.challenge.business.service.match.impl;

import gg.bayes.challenge.business.service.match.MatchLogQueryService;
import gg.bayes.challenge.common.constants.Constants;
import gg.bayes.challenge.config.rest.model.HeroDamage;
import gg.bayes.challenge.config.rest.model.HeroItem;
import gg.bayes.challenge.config.rest.model.HeroKills;
import gg.bayes.challenge.config.rest.model.HeroSpells;
import gg.bayes.challenge.persistence.model.CombatLogEntryEntity;
import gg.bayes.challenge.persistence.repository.CombatLogEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Service
@RequiredArgsConstructor
public class MatchLogQueryServiceImpl implements MatchLogQueryService {

	private final CombatLogEntryRepository combatLogEntryRepository;

	@Override
	public List<HeroKills> getHeroKillsByMatch(Long matchId) {
		var combatLog = combatLogEntryRepository.findAllByMatchIdAndType(matchId,
				CombatLogEntryEntity.Type.HERO_KILLED);
		Map<String, Long> heroKillsMap = combatLog.stream()
				.collect(Collectors.groupingBy(CombatLogEntryEntity::getActor, Collectors.counting()));
		var response = new LinkedList<HeroKills>();
		heroKillsMap.forEach((k, v) -> response.add(new HeroKills(k, v.intValue())));
		return response;
	}

	@Override
	public List<HeroSpells> getHeroSpellsByMatchAndHero(Long matchId, String heroName) {
		heroName = pruneHeroName(heroName);
		var combatLog = getCombatLogsByCriteria(matchId, heroName, CombatLogEntryEntity.Type.SPELL_CAST);
		Map<String, Long> heroSpellsMap = combatLog.stream()
				.collect(Collectors.groupingBy(CombatLogEntryEntity::getAbility, Collectors.counting()));
		var response = new LinkedList<HeroSpells>();
		heroSpellsMap.forEach((k, v) -> response.add(new HeroSpells(k, v.intValue())));
		return response;
	}

	private String pruneHeroName(String heroName) {
		if (heroName.contains(Constants.HERO_KEYWORD)) {
			heroName = heroName.replace(Constants.HERO_KEYWORD, "");
		}
		return heroName;
	}

	@Override
	public List<HeroDamage> getHeroDamageByMatch(Long matchId, String heroName) {
		heroName = pruneHeroName(heroName);
		var combatLog = getCombatLogsByCriteria(matchId, heroName, CombatLogEntryEntity.Type.DAMAGE_DONE);
		Map<String, AbstractMap.SimpleEntry<Integer, Long>> map = combatLog.stream().collect(toMap(
				CombatLogEntryEntity::getTarget, b -> new AbstractMap.SimpleEntry<>(b.getDamage(), 1L),
				(v1, v2) -> new AbstractMap.SimpleEntry<>(v1.getKey() + v2.getKey(), v1.getValue() + v2.getValue())));
		var response = new LinkedList<HeroDamage>();
		map.forEach((k, v) -> {
			response.add(new HeroDamage(k, v.getValue().intValue(), v.getKey()));
		});
		return response;
	}

	@Override
	public List<HeroItem> getHeroItemsByMatch(Long matchId, String heroName) {
		heroName = pruneHeroName(heroName);
		var combatLog = getCombatLogsByCriteria(matchId, heroName, CombatLogEntryEntity.Type.ITEM_PURCHASED);
		return combatLog.stream().map(item -> new HeroItem(item.getItem(), item.getTimestamp()))
				.collect(Collectors.toList());
	}

	private List<CombatLogEntryEntity> getCombatLogsByCriteria(Long matchId, String heroName,
			CombatLogEntryEntity.Type type) {
		return combatLogEntryRepository.findAllByMatchIdAndActorAndType(matchId, heroName, type);
	}
}

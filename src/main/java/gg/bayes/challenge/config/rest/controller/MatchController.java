package gg.bayes.challenge.config.rest.controller;

import gg.bayes.challenge.business.service.match.MatchLogCommandService;
import gg.bayes.challenge.business.service.match.MatchLogQueryService;
import gg.bayes.challenge.config.rest.model.HeroDamage;
import gg.bayes.challenge.config.rest.model.HeroItem;
import gg.bayes.challenge.config.rest.model.HeroKills;
import gg.bayes.challenge.config.rest.model.HeroSpells;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/match")
@Validated
@RequiredArgsConstructor
public class MatchController {

	/**
	 * Ingests a DOTA combat log file, parses and persists relevant events data. All
	 * events are associated with the same match id.
	 *
	 * @param combatLog the content of the combat log file
	 * @return the match id associated with the parsed events
	 */

	private final MatchLogCommandService matchLogCommandService;
	private final MatchLogQueryService matchLogQueryService;

	@PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<Long> ingestCombatLog(@RequestBody String combatLog) {
		return ResponseEntity.ok(matchLogCommandService.importData(combatLog));
	}

	/**
	 *
	 * Fetches the heroes and their kill counts for the given match.
	 *
	 * @param matchId the match identifier
	 * @return a collection of heroes and their kill counts
	 */
	@GetMapping(path = "{matchId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<HeroKills>> getMatch(@PathVariable("matchId") Long matchId) {
		return ResponseEntity.ok(matchLogQueryService.getHeroKillsByMatch(matchId));

	}

	/**
	 * For the given match, fetches the items bought by the named hero.
	 *
	 * @param matchId  the match identifier
	 * @param heroName the hero name
	 * @return a collection of items bought by the hero during the match
	 */
	@GetMapping(path = "{matchId}/{heroName}/items", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<HeroItem>> getHeroItems(@PathVariable("matchId") Long matchId,
			@PathVariable("heroName") String heroName) {
		return ResponseEntity.ok(matchLogQueryService.getHeroItemsByMatch(matchId,heroName));
	}

	/**
	 * For the given match, fetches the spells cast by the named hero.
	 *
	 * @param matchId  the match identifier
	 * @param heroName the hero name
	 * @return a collection of spells cast by the hero and how many times they were
	 *         cast
	 */
	@GetMapping(path = "{matchId}/{heroName}/spells", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<HeroSpells>> getHeroSpells(@PathVariable("matchId") Long matchId,
			@PathVariable("heroName") String heroName) {
		return ResponseEntity.ok(matchLogQueryService.getHeroSpellsByMatchAndHero(matchId, heroName));
	}

	/**
	 * For a given match, fetches damage done data for the named hero.
	 *
	 * @param matchId  the match identifier
	 * @param heroName the hero name
	 * @return a collection of "damage done" (target, number of times and total
	 *         damage) elements
	 */
	@GetMapping(path = "{matchId}/{heroName}/damage", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<HeroDamage>> getHeroDamages(@PathVariable("matchId") Long matchId,
			@PathVariable("heroName") String heroName) {
		return ResponseEntity.ok(matchLogQueryService.getHeroDamageByMatch(matchId, heroName));
	}
}

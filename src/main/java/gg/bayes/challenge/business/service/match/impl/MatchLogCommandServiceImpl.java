package gg.bayes.challenge.business.service.match.impl;

import gg.bayes.challenge.business.service.match.MatchLogCommandService;
import gg.bayes.challenge.common.constants.Constants;
import gg.bayes.challenge.common.file.FileUtils;
import gg.bayes.challenge.persistence.model.CombatLogEntryEntity;
import gg.bayes.challenge.persistence.model.MatchEntity;
import gg.bayes.challenge.persistence.repository.MatchRepository;
import io.swagger.models.auth.In;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchLogCommandServiceImpl implements MatchLogCommandService {

	private final MatchRepository matchRepository;

	@Override
	@Transactional
	public Long importData(String combatLog) {
		combatLog = FileUtils.handleSpecialCharacters(combatLog);
		List<String> splitLog = pruneLog(combatLog);
		if (splitLog.isEmpty()) {
			throw new RuntimeException("There is no data to process for the related file.");
		}
		return processPureData(splitLog);
	}

	private List<String> pruneLog(String combatLog) {
		List<String> splitLog = Arrays.asList(combatLog.split("~"));
		splitLog = splitLog.parallelStream()
				.filter(item -> StringUtils.hasLength(item)
						&& (item.contains(Constants.CAST_KEYWORD) || item.contains(Constants.KILL_KEYWORD)
								|| item.contains(Constants.BUY_KEYWORD) || item.contains(Constants.HIT_KEYWORD)))
				.collect(Collectors.toList());

		splitLog = splitLog.parallelStream().map(item -> {
			if (item.contains(Constants.HERO_KEYWORD) || item.contains(Constants.ITEM_KEYWORD)) {
				item = item.replace(Constants.HERO_KEYWORD, "");
				item = item.replace(Constants.ITEM_KEYWORD, "");
			}
			return item;
		}).collect(Collectors.toList());

		return splitLog;
	}

	private Long processPureData(List<String> splitLog) {
		var matchEntity = prepareMatchLogEntity(splitLog);
		return matchRepository.save(matchEntity).getId();
	}

	private MatchEntity prepareMatchLogEntity(List<String> splitLog) {

		var matchedEntity = new MatchEntity();
		Set<CombatLogEntryEntity> combatLogEntryEntitySet = new HashSet<>();
		splitLog.stream().forEach(item -> {
			var combatLogEntryEntity = new CombatLogEntryEntity();
			if (item.contains(Constants.KILL_KEYWORD)) {
				setKillSection(item, combatLogEntryEntity, matchedEntity, combatLogEntryEntitySet);
			} else if (item.contains(Constants.BUY_KEYWORD)) {
				setBuySection(item, combatLogEntryEntity, matchedEntity, combatLogEntryEntitySet);
			} else if (item.contains(Constants.CAST_KEYWORD)) {
				setCastSection(item, combatLogEntryEntity, matchedEntity, combatLogEntryEntitySet);
			} else if (item.contains(Constants.HIT_KEYWORD)) {
				setHitSection(item, combatLogEntryEntity, matchedEntity, combatLogEntryEntitySet);
			}

		});
		matchedEntity.setCombatLogEntries(combatLogEntryEntitySet);
		return matchedEntity;
	}

	private void setKillSection(String item, CombatLogEntryEntity combatLogEntryEntity, MatchEntity matchedEntity,
			Set<CombatLogEntryEntity> combatLogEntryEntitySet) {
		if (!item.contains(Constants.NON_HERO_KEYWORD)) {
			final String regex = "\\w+?(?= is killed by)";
			final String regex2 = "(?<= is killed by )\\w+";

			final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
			final Matcher matcher = pattern.matcher(item);

			final Pattern pattern2 = Pattern.compile(regex2, Pattern.MULTILINE);
			final Matcher matcher2 = pattern2.matcher(item);

			combatLogEntryEntity.setActor(matcher2.find() ? matcher2.group(0) : null);
			combatLogEntryEntity.setTarget(matcher.find() ? matcher.group(0) : null);
			combatLogEntryEntity.setType(CombatLogEntryEntity.Type.HERO_KILLED);
			combatLogEntryEntity.setTimestamp(findTimeStamp(item));
			combatLogEntryEntity.setMatch(matchedEntity);
			combatLogEntryEntitySet.add(combatLogEntryEntity);
		}
	}

	private void setBuySection(String item, CombatLogEntryEntity combatLogEntryEntity, MatchEntity matchedEntity,
			Set<CombatLogEntryEntity> combatLogEntryEntitySet) {
		final String regex = "\\w+?(?= buys)";
		final String regex2 = "(?<= item )\\w+";

		final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
		final Matcher matcher = pattern.matcher(item);

		final Pattern pattern2 = Pattern.compile(regex2, Pattern.MULTILINE);
		final Matcher matcher2 = pattern2.matcher(item);

		combatLogEntryEntity.setActor(matcher.find() ? matcher.group(0) : null);
		combatLogEntryEntity.setItem(matcher2.find() ? matcher2.group(0) : null);
		combatLogEntryEntity.setType(CombatLogEntryEntity.Type.ITEM_PURCHASED);
		combatLogEntryEntity.setTimestamp(findTimeStamp(item));
		combatLogEntryEntity.setMatch(matchedEntity);
		combatLogEntryEntitySet.add(combatLogEntryEntity);
	}

	private void setCastSection(String item, CombatLogEntryEntity combatLogEntryEntity, MatchEntity matchedEntity,
			Set<CombatLogEntryEntity> combatLogEntryEntitySet) {
		final String regex = "\\w+?(?= casts ability)";
		final String regex2 = "(?<= lvl )[1-9]";
		final String regex3 = "(?<= ability )\\w+";

		final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
		final Matcher matcher = pattern.matcher(item);

		final Pattern pattern2 = Pattern.compile(regex2, Pattern.MULTILINE);
		final Matcher matcher2 = pattern2.matcher(item);

		final Pattern pattern3 = Pattern.compile(regex3, Pattern.MULTILINE);
		final Matcher matcher3 = pattern3.matcher(item);

		combatLogEntryEntity.setActor(matcher.find() ? matcher.group(0) : null);
		combatLogEntryEntity.setAbilityLevel(matcher2.find() ? Integer.parseInt(matcher2.group(0)) : null);
		combatLogEntryEntity.setAbility(matcher3.find() ? matcher3.group(0) : null);
		combatLogEntryEntity.setType(CombatLogEntryEntity.Type.SPELL_CAST);
		combatLogEntryEntity.setTimestamp(findTimeStamp(item));
		combatLogEntryEntity.setMatch(matchedEntity);
		combatLogEntryEntitySet.add(combatLogEntryEntity);
	}

	private void setHitSection(String item, CombatLogEntryEntity combatLogEntryEntity, MatchEntity matchedEntity,
			Set<CombatLogEntryEntity> combatLogEntryEntitySet) {
		final String regex = "\\w+?(?= hits)";
		final String regex2 = "(?<= hits )\\w+";
		final String regex3 = "(?<= with )\\w+";
		final String regex4 = "(?<= for )[1-9]+";

		final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
		final Matcher matcher = pattern.matcher(item);

		final Pattern pattern2 = Pattern.compile(regex2, Pattern.MULTILINE);
		final Matcher matcher2 = pattern2.matcher(item);

		final Pattern pattern3 = Pattern.compile(regex3, Pattern.MULTILINE);
		final Matcher matcher3 = pattern3.matcher(item);

		final Pattern pattern4 = Pattern.compile(regex4, Pattern.MULTILINE);
		final Matcher matcher4 = pattern4.matcher(item);

		combatLogEntryEntity.setActor(matcher.find() ? matcher.group(0) : null);
		combatLogEntryEntity.setTarget(matcher2.find() ? matcher2.group(0) : null);
		combatLogEntryEntity.setAbility(matcher3.find() ? matcher3.group(0) : null);
		combatLogEntryEntity.setDamage(matcher4.find() ? Integer.parseInt(matcher4.group(0)) : null);
		combatLogEntryEntity.setType(CombatLogEntryEntity.Type.DAMAGE_DONE);
		combatLogEntryEntity.setTimestamp(findTimeStamp(item));
		combatLogEntryEntity.setMatch(matchedEntity);
		combatLogEntryEntitySet.add(combatLogEntryEntity);
	}

	private Long findTimeStamp(String item) {
		final String regex = "\\d[0-9]+";
		final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
		final Matcher matcher = pattern.matcher(item);
		int index = 0;
		long result = 0;
		while (matcher.find()) {
			if (index == 0) {
				result += Long.parseLong(matcher.group(0)) * 3600 * 1000;
			} else if (index == 1) {
				result += Long.parseLong(matcher.group(0)) * 60 * 1000;
			} else if (index == 2) {
				result += Long.parseLong(matcher.group(0)) * 1000;
			} else if (index == 3) {
				result += Long.parseLong(matcher.group(0));
			}
			index += 1;
		}

		return result;
	}

}

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
        splitLog.parallelStream().forEach(item -> {
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
        final String regexForBuyKeyword = "\\w+?(?= buys)";
        final String regexForItemKeyword = "(?<= item )\\w+";

        final Pattern patternOfBuy = Pattern.compile(regexForBuyKeyword, Pattern.MULTILINE);
        final Matcher matcherOfBuy = patternOfBuy.matcher(item);

        final Pattern patternOfItem = Pattern.compile(regexForItemKeyword, Pattern.MULTILINE);
        final Matcher matcherOfItem = patternOfItem.matcher(item);

        combatLogEntryEntity.setActor(matcherOfBuy.find() ? matcherOfBuy.group(0) : null);
        combatLogEntryEntity.setItem(matcherOfItem.find() ? matcherOfItem.group(0) : null);
        combatLogEntryEntity.setType(CombatLogEntryEntity.Type.ITEM_PURCHASED);
        combatLogEntryEntity.setTimestamp(findTimeStamp(item));
        combatLogEntryEntity.setMatch(matchedEntity);
        combatLogEntryEntitySet.add(combatLogEntryEntity);
    }

    private void setCastSection(String item, CombatLogEntryEntity combatLogEntryEntity, MatchEntity matchedEntity,
                                Set<CombatLogEntryEntity> combatLogEntryEntitySet) {
        final String regexForCast = "\\w+?(?= casts ability)";
        final String regexForLevel = "(?<= lvl )[1-9]";
        final String regexForAbility = "(?<= ability )\\w+";

        final Pattern patternOfCast = Pattern.compile(regexForCast, Pattern.MULTILINE);
        final Matcher matcherOfCast = patternOfCast.matcher(item);

        final Pattern patternOfLevel = Pattern.compile(regexForLevel, Pattern.MULTILINE);
        final Matcher matcherOfLevel = patternOfLevel.matcher(item);

        final Pattern patternOfAbility = Pattern.compile(regexForAbility, Pattern.MULTILINE);
        final Matcher matcherOfAbility = patternOfAbility.matcher(item);

        combatLogEntryEntity.setActor(matcherOfCast.find() ? matcherOfCast.group(0) : null);
        combatLogEntryEntity.setAbilityLevel(matcherOfLevel.find() ? Integer.parseInt(matcherOfLevel.group(0)) : null);
        combatLogEntryEntity.setAbility(matcherOfAbility.find() ? matcherOfAbility.group(0) : null);
        combatLogEntryEntity.setType(CombatLogEntryEntity.Type.SPELL_CAST);
        combatLogEntryEntity.setTimestamp(findTimeStamp(item));
        combatLogEntryEntity.setMatch(matchedEntity);
        combatLogEntryEntitySet.add(combatLogEntryEntity);
    }

    private void setHitSection(String item, CombatLogEntryEntity combatLogEntryEntity, MatchEntity matchedEntity,
                               Set<CombatLogEntryEntity> combatLogEntryEntitySet) {
        final String regexForHitKeyword = "\\w+?(?= hits)";
        final String regexForAfterHitKeyword = "(?<= hits )\\w+";
        final String regexForWithKeyword = "(?<= with )\\w+";
        final String regexForAfterKeyword = "(?<= for )[1-9]+";

        final Pattern patternOfHit = Pattern.compile(regexForHitKeyword, Pattern.MULTILINE);
        final Matcher matcherOfHit = patternOfHit.matcher(item);

        final Pattern patternOfAfterHit = Pattern.compile(regexForAfterHitKeyword, Pattern.MULTILINE);
        final Matcher matcherOfAfterHit = patternOfAfterHit.matcher(item);

        final Pattern patternOfWith = Pattern.compile(regexForWithKeyword, Pattern.MULTILINE);
        final Matcher matcherOfWith = patternOfWith.matcher(item);

        final Pattern patternOfAfterWith = Pattern.compile(regexForAfterKeyword, Pattern.MULTILINE);
        final Matcher matcherOfAfterWith = patternOfAfterWith.matcher(item);

        combatLogEntryEntity.setActor(matcherOfHit.find() ? matcherOfHit.group(0) : null);
        combatLogEntryEntity.setTarget(matcherOfAfterHit.find() ? matcherOfAfterHit.group(0) : null);
        combatLogEntryEntity.setAbility(matcherOfWith.find() ? matcherOfWith.group(0) : null);
        combatLogEntryEntity.setDamage(matcherOfAfterWith.find() ? Integer.parseInt(matcherOfAfterWith.group(0)) : null);
        combatLogEntryEntity.setType(CombatLogEntryEntity.Type.DAMAGE_DONE);
        combatLogEntryEntity.setTimestamp(findTimeStamp(item));
        combatLogEntryEntity.setMatch(matchedEntity);
        combatLogEntryEntitySet.add(combatLogEntryEntity);
    }

    private Long findTimeStamp(String item) {
        final String regexForTimeValue = "\\d[0-9]+";
        final Pattern patternOfTime = Pattern.compile(regexForTimeValue, Pattern.MULTILINE);
        final Matcher matcherOfTime = patternOfTime.matcher(item);
        int index = 0;
        long result = 0;

        while (matcherOfTime.find()) {
            if (index == 0) {
                result += Long.parseLong(matcherOfTime.group(0)) * Constants.HOUR_EQ_SECONDS * Constants.MILLISECOND_MULTIPLIER;
            } else if (index == 1) {
                result += Long.parseLong(matcherOfTime.group(0)) * Constants.MINUTE_EQ_SECONDS * Constants.MILLISECOND_MULTIPLIER;
            } else if (index == 2) {
                result += Long.parseLong(matcherOfTime.group(0)) * Constants.MILLISECOND_MULTIPLIER;
            } else if (index == 3) {
                result += Long.parseLong(matcherOfTime.group(0));
                break;
            }
            index += 1;
        }

        return result;
    }

}

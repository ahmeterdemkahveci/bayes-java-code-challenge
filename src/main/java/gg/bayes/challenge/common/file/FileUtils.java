package gg.bayes.challenge.common.file;

public class FileUtils {

	public static String handleSpecialCharacters(String combatLog) {
		combatLog = combatLog.replaceAll("\\[", "~");
		combatLog = combatLog.replaceAll("\\]", "");
		combatLog = combatLog.replaceAll("\\(", "");
		combatLog = combatLog.replaceAll("\\)", "");
		return combatLog;

	}

}

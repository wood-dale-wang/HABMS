package HABMS.db;

import java.security.SecureRandom;
import java.time.LocalDate;

/** ID生成静态方法集合 */
final class IdGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    private IdGenerator() {
    }

    static String newAid() {
        return formatNumeric(10);
    }

    static String newDid() {
        return formatNumeric(8);
    }

    static String newApid() {
        return formatNumeric(12);
    }

    static int newSid() {
        int value = RANDOM.nextInt(Integer.MAX_VALUE);
        return value == 0 ? 1 : value; // avoid zero to keep it visually distinct
    }

    private static String formatNumeric(int width) {
        long prefix = LocalDate.now().getDayOfYear();
        long randomPart = Math.abs(RANDOM.nextLong());
        long value = prefix * 1_000_000L + randomPart % 1_000_000L;
        String formatted = Long.toString(value);
        if (formatted.length() > width) {
            formatted = formatted.substring(formatted.length() - width);
        }
        return String.format("%0" + width + "d", Long.parseLong(formatted));
    }
}

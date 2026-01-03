package HABMS.db;

import java.security.SecureRandom;
import java.time.LocalDate;

/** ID 生成工具：为账号、医生、预约、排班生成近似唯一的编号。 */
final class IdGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    private IdGenerator() {
    }

    /** 生成 10 位患者编号。 */
    static String newAid() {
        return formatNumeric(10);
    }

    /** 生成 8 位医生编号。 */
    static String newDid() {
        return formatNumeric(8);
    }

    /** 生成 12 位预约号：yyMMdd + 随机 6 位。 */
    static String newApid() {
        // 12 digits: yyMMdd (6) + random (6)
        String datePart = java.time.format.DateTimeFormatter.ofPattern("yyMMdd").format(LocalDate.now());
        long randomPart = Math.abs(RANDOM.nextLong()) % 1_000_000L;
        return datePart + String.format("%06d", randomPart);
    }

    /** 生成正整数排班编号，避开 0。 */
    static int newSid() {
        int value = RANDOM.nextInt(Integer.MAX_VALUE);
        return value == 0 ? 1 : value; // avoid zero to keep it visually distinct
    }

    /**
     * 生成固定宽度的数字串：以当年日序为前缀，后接随机数，超过宽度时截断末尾。
     */
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

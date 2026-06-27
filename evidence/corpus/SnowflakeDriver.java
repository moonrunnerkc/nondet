/**
 * Drives one id generation from the real SnowFlake class, so the campaign exercises external code.
 *
 * <p>It builds a SnowFlake with a fixed datacenter and machine id and prints one generated id. With
 * those ids fixed and the sequence starting at zero, the id is a function of the one
 * System.currentTimeMillis read inside SnowFlake, which is the read nondet attributes. SnowFlake is
 * compiled from a pinned upstream commit alongside this driver; the driver lives here so the corpus
 * is reproducible.
 */
public final class SnowflakeDriver {

    private SnowflakeDriver() {
    }

    /**
     * Prints one snowflake id from the real generator.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        System.out.println(new SnowFlake(2, 3).nextId());
    }
}

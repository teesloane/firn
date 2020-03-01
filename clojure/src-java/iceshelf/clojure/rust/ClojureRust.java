package iceshelf.clojure.rust;

public class ClojureRust {
    // This declares that the static `getFreeMemory` method will be provided
    // a native library.
    private static native String getFreeMemoryRust(String unit);

    // The rest is just regular ol' Java!

    public static String getFreeMemory(String unit) throws java.io.IOException {
        String output = getFreeMemoryRust(unit);
        return output;
    }
}

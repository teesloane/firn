package iceshelf.clojure.rust;

public class ClojureRust {
    // This declares that the static `parseOrg` method will be provided
    // a native library.
    private static native String parseOrgRust(String unit);

    // The rest is just regular ol' Java!

    public static String parseOrg(String unit) throws java.io.IOException {
        String output = parseOrgRust(unit);
        return output;
    }
}

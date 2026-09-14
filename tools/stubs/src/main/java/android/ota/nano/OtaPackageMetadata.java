package android.ota.nano;

public class OtaPackageMetadata {
    public static class OtaMetadata {
        public static final int UNKNOWN = 0;
        public static final int AB = 1;
        public static final int BLOCK = 2;
        public int type = 0;
        public Postcondition postcondition = new Postcondition();

        public static class Postcondition {
            public String sdkLevel = "0";
            public String securityPatchLevel = "";
            public long timestamp = 0L;
        }

        public static OtaMetadata parseFrom(byte[] data) {
            return new OtaMetadata();
        }
    }
}

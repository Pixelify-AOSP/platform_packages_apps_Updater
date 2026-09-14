package android.os;

import android.content.res.AssetFileDescriptor;

public class UpdateEngine {
    public static final class UpdateStatusConstants {
        public static final int IDLE = 0;
        public static final int CHECKING_FOR_UPDATE = 1;
        public static final int UPDATE_AVAILABLE = 2;
        public static final int DOWNLOADING = 3;
        public static final int VERIFYING = 4;
        public static final int FINALIZING = 5;
        public static final int UPDATED_NEED_REBOOT = 6;
        public static final int REPORTING_ERROR_EVENT = 7;
        public static final int ATTEMPTING_ROLLBACK = 8;
        public static final int DISABLED = 9;
    }

    public static final class ErrorCodeConstants {
        public static final int SUCCESS = 0;
        public static final int ERROR = 1;
    }

    public boolean bind(UpdateEngineCallback callback) { return true; }
    public boolean unbind() { return true; }
    public void applyPayload(String url, long offset, long size, String[] headerKeyValuePairs) {}
    public void applyPayload(AssetFileDescriptor pfd, String[] headerKeyValuePairs) {}
    public void cancel() {}
    public void suspend() {}
    public void resume() {}
    public void resetStatus() {}
    public void setPerformanceMode(boolean enable) {}
}

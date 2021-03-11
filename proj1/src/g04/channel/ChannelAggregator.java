package g04.channel;

import java.io.IOException;

public class ChannelAggregator {

    private final ControlChannel controlChannel;
    private final BackupChannel backupChannel;
    private final RestoreChannel restoreChannel;

    public ChannelAggregator(String mcAddress, int mcPort, String mdbAddress, int mdbPort, String mdrAddress, int mdrPort)
            throws IOException {

        this.controlChannel = new ControlChannel(mcAddress, mcPort);
        this.backupChannel = new BackupChannel(mdbAddress, mdbPort);
        this.restoreChannel = new RestoreChannel(mdrAddress, mdrPort);

    }

    public ControlChannel getControlChannel() {
        return controlChannel;
    }

    public BackupChannel getBackupChannel() {
        return backupChannel;
    }

    public RestoreChannel getRestoreChannel() {
        return restoreChannel;
    }
}
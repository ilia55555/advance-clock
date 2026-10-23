package com.ilia.advanceclock;

/** Controls only the currently ringing occurrence; never changes saved alert preferences. */
interface AlertOutput {
    boolean muted();
    boolean vibrating();
    void mute(boolean muted);
    void vibrate(boolean enabled);

    static AlertOutput alarm(long id) {
        return new AlertOutput() {
            public boolean muted() { return AlarmSoundService.outputMuted(id); }
            public boolean vibrating() { return AlarmSoundService.outputVibrating(id); }
            public void mute(boolean value) { AlarmSoundService.setOutputMuted(id,value); }
            public void vibrate(boolean value) { AlarmSoundService.setOutputVibrating(id,value); }
        };
    }
    static AlertOutput note(long id) {
        return new AlertOutput() {
            public boolean muted() { return NoForgetSoundService.outputMuted(id); }
            public boolean vibrating() { return NoForgetSoundService.outputVibrating(id); }
            public void mute(boolean value) { NoForgetSoundService.setOutputMuted(id,value); }
            public void vibrate(boolean value) { NoForgetSoundService.setOutputVibrating(id,value); }
        };
    }
    static AlertOutput adhan(int type) {
        return new AlertOutput() {
            public boolean muted() { return AdhanSoundService.outputMuted(type); }
            public boolean vibrating() { return AdhanSoundService.outputVibrating(type); }
            public void mute(boolean value) { AdhanSoundService.setOutputMuted(type,value); }
            public void vibrate(boolean value) { AdhanSoundService.setOutputVibrating(type,value); }
        };
    }
}

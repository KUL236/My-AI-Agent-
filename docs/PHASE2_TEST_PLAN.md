# Manual verification checklist (v0.3.0)

## Android smoke test
- [ ] Gradle sync succeeds with JDK 17 and SDK 36.
- [ ] App launches without crashing on Android 8.0+; content is not hidden under status/nav bars on Android 15.
- [ ] Microphone permission prompt appears on first voice use.
- [ ] Speech recognition returns recognized text on a device with an ASR service.
- [ ] Text input submits a message and UI remains responsive.
- [ ] "open youtube", "open chrome", "open whatsapp" launch only when installed; unknown app gives a safe message.
- [ ] Settings, Wi-Fi, Bluetooth and Camera open or show a safe unavailable message.
- [ ] Flashlight on/off, volume up/down, "set timer 1 minute" work.
- [ ] Battery and time commands return device values; "sometimes" does NOT trigger time.
- [ ] Recent history remains after app restart; Clear History empties it.

## Phone control (Accessibility)
- [ ] Without the service enabled, "go home" explains how to enable it.
- [ ] "enable phone control" opens Accessibility settings; service can be enabled (Allow restricted settings on Android 13+).
- [ ] go home / go back / recent apps / notifications / quick settings / lock screen / screenshot work.
- [ ] Open a long list in another app, then "scroll down" scrolls it.
- [ ] "tap <visible text>" asks for confirmation; "no" cancels; "yes" taps.
- [ ] SEWA never taps or scrolls inside its own window.

## Security acceptance gates before wider release
- [ ] Owner enrollment with informed consent and secure template storage.
- [ ] Speaker verification evaluated for false accept / false reject rates.
- [ ] Liveness and replay / synthetic voice attack testing.
- [ ] Android biometric or PIN fallback before sensitive actions.
- [ ] Privacy review, retention controls and deletion workflow.

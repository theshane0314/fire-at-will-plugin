# Fire at will Timer

Tracks how long the **Fire at will!** crew call remains active during boat combat (Sailing skill) and notifies you before it expires.

## Features

- Live countdown timer (large tile or small tile / infobox)
- Color thresholds — turns yellow at the warning threshold and red at the alert threshold
- Configurable notification at the warning, alert, or expiry moment — uses RuneLite's standard notification panel for full control over sound, flash, color, system tray, and chat options
- Detects manual cancellation when you swap to **Attack my targets!** or **Await further orders!**
- 5 minute linger after expiry so you don't lose track of when the call ended
- Resets cleanly on logout / world hop / connection loss

## How it works

The plugin watches public chat for the **Fire at will!** call from your character. Once detected, a 10 minute timer begins. The timer ends when:

- 10 minutes elapse (expiry — fires the configured notification)
- The chat message `your crew takes a break from firing at will` appears (natural end)
- You issue **Attack my targets!** or **Await further orders!** (manual override — no expiry alert)

## Configuration

- **Display Mode** — large tile (OverlayPanel) or small tile (InfoBox)
- **Warning / Alert Thresholds** — when the timer changes color (default 30s / 10s)
- **Notify On** — fire the notification at warning, alert, or expiry (default expiry)
- **Notification** — open the notification override panel for sound / flash / color / tray / chat options

# MoPay NFC Demo - Professional + Supabase

This prototype contains two Android apps:

- Customer app: Supabase wallet/card dashboard + card controls + demo HCE card emulation.
- Merchant app: Supabase merchant dashboard + NFC reader + ISO-DEP/contactless detection + demo payment.

## Supabase
Run `mopay-supabase-app-access.sql` in Supabase SQL Editor after the foundation schema. It includes the customer read RPCs, payment RPC, card status, PIN change, demo top-up and demo send actions.

The Android apps use the Supabase publishable key only. Never put a Supabase secret/service-role key in an APK.

## NFC demo
The Merchant app listens for NFC-A, NFC-B, NFC-F and NFC-V. It maps a detected NFC object to `CARD_DEMO_4821` for the prototype.

- A compatible contactless bank card may be detected as ISO-DEP/contactless. The app does not read or store PAN, CVV, expiry or payment credentials.
- A compatible Android phone running the Customer app can emulate the MoPay demo card using Android HCE. Enable NFC on both phones and keep the Customer app installed/enabled.
- A real secure NFC card should replace demo mapping for production.

## Demo PIN
Initial demo PIN: `1234`

## GitHub Actions
Keep the existing `.github/workflows/main.yml` in the repository. Upload/replace the application source folders and root files; do not delete the working workflow.

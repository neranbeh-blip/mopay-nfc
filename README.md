# MoPay NFC Demo

Professional MTN-inspired customer + merchant Android prototype for the MoPay NFC concept.

## Included
- `customer-app`: customer wallet dashboard
- `merchant-app`: merchant NFC payment terminal
- Supabase REST RPC integration using the project publishable key
- Customer balance, card status, freeze/unfreeze, transaction history
- Merchant amount entry, NFC card detection, PIN authorization, payment processing, daily summary
- Black / MTN-yellow / white visual system
- MoPay launcher icon/logo

## Supabase
The apps call the following RPCs:
- `get_demo_customer_snapshot`
- `get_demo_merchant_dashboard`
- `set_demo_card_status`
- `process_demo_payment`

Run `mopay-supabase-app-access.sql` in Supabase SQL Editor after the foundation SQL.

The client uses the **publishable** key only. Never put a Supabase secret/service-role key in an APK.

## Demo credentials
- Customer: NGOH ERAN
- Demo card: `CARD_DEMO_4821` / displayed as `•••• 4821`
- PIN: `1234`
- Demo merchant ID: `22222222-2222-2222-2222-222222222222`

## Important
This is a prototype. The payment RPC only debits the demo Supabase wallet. It does not move real MTN Mobile Money funds. Production requires authenticated users, stronger card authentication, server-side authorization, transaction limits/risk controls, and approved operator APIs.

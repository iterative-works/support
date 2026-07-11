---
layout_id: public
last_verified: 2026-07-11
---

# Public layout (scenario shell)

Every scenario page is served inside the same minimal shell — no navigation,
no auth chrome.

## structure

- `<head>` declares `<meta charset="utf-8">`, an inline stylesheet, and the
  htmx 2.0.2 CDN script. Responses carry
  `Content-Type: text/html; charset=utf-8` (both are load-bearing: without
  them browsers submit forms as Latin-1).
- `<body>` contains the scenario content directly; forms POST to the
  scenario's own `/form` route.

## known cosmetics

- `/favicon.ico` 404s — one console error on every page load, harmless.

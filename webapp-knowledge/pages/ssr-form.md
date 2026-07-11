---
page_id: ssr-form
url_pattern: /ssrForm/page
url_params: []
layout: public
roles: [anonymous]
tags: [forms, ssr, htmx]
last_verified: 2026-07-11
---

# SSR form scenario ("inquiry" proof form)

## purpose

Proof of the UIForm SSR loop (UIFormHtmlRenderer + RequiredValidation):
a rich form covering conditional sections, required validation, repeated
rows with add/remove, hidden state round-tripping, and a success page.
This is the regression surface for the forms-consolidation SSR slice.

## preconditions

- None. Stateless — every GET renders the empty form with defaults
  (kind=quote, one item row `i1`).

## test_data_setup

Not applicable — no persistence; all state lives in the submitted form.

## elements

### form chrome
| id | selector | type | description |
|----|----------|------|-------------|
| form | form#inquiry | form | POSTs to /ssrForm/form; htmx outerHTML-swaps itself on committed-choice change; has `novalidate` |
| submit | button[name='__submit'] | submit | Real submission (server distinguishes it from re-renders/actions) |

### customer section
| id | selector | type | description |
|----|----------|------|-------------|
| name | input[name='inquiry.customer.name'] | text | Required; label "Name" |
| email | input[name='inquiry.customer.email'] | email | Required; label "E-mail" |
| note | textarea[name='inquiry.customer.note'] | textarea | Optional prose |
| token | input[name='inquiry.token'] | hidden | Constant "proof" |

### request section
| id | selector | type | description |
|----|----------|------|-------------|
| kind | select[name='inquiry.request.kind'] | select | quote / order; changing it triggers HTMX re-render |
| delivery_address | input[name='inquiry.request.delivery.address'] | text | Required; ONLY present when kind=order (ShowIf) |
| deadline | input[name='inquiry.request.deadline'] | date | Optional (Date is forced optional in the ADT) |
| summary | #inquiry-request-summary | display | Server-rendered text, e.g. "a order with 2 item(s)"; updates on re-render |

### repeated items
| id | selector | type | description |
|----|----------|------|-------------|
| items_state | input[name='inquiry.items.__items'] | hidden | One per row, value "key:type" (e.g. "i1:row") |
| qty | input[name='inquiry.items.{key}.row.qty'] | number | Required per row; {key} is i1, i2, … |
| desc | input[name='inquiry.items.{key}.row.desc'] | text | Required per row |
| remove | button[name='inquiry.items.{key}.row.remove'] | submit | Server action: full-page POST, removes that row |
| add_item | button[name='inquiry.controls.addItem'] | submit | Server action: full-page POST, appends a row |

### errors
| id | selector | type | description |
|----|----------|------|-------------|
| field_errors | .field-errors .field-error | text | Rendered inside the offending field's div.field; human labels ("Please fill in Name") |

## actions

### reveal_delivery_section
description: Select "order" and wait for the HTMX fragment swap
steps:
  - select: { selector: "select[name='inquiry.request.kind']", value: "order" }
  - wait_for: { text: "Delivery" }
  - verify: { selector: "input[name='inquiry.request.delivery.address']", state: visible }
notes: Exactly one POST /ssrForm/form fires; URL stays on /ssrForm/page.

### submit_blank_expect_errors
description: Submit the empty form and observe server-side required errors
steps:
  - click: "button[name='__submit']"
  - wait_for: { strategy: page_load }
  - verify: { selector: ".field-errors .field-error", state: visible }
notes: Full-page POST to /ssrForm/form; errors render next to each field.

### add_row
description: Append a repeated item row (values in other fields survive)
steps:
  - click: "button[name='inquiry.controls.addItem']"
  - wait_for: { strategy: page_load }
  - verify: count of "input[name='inquiry.items.__items']" increased

### remove_row
description: Remove one row; the other rows' data must survive
params: [key]
steps:
  - click: "button[name='inquiry.items.${key}.row.remove']"
  - wait_for: { strategy: page_load }
  - verify: "input[name='inquiry.items.${key}.row.qty']" absent, others intact

### submit_success
description: Submit a validly filled form
steps:
  - click: "button[name='__submit']"
  - wait_for: { text: "Inquiry received" }
returns:
  data_dump: success page renders the submitted FormR as JSON-ish text

## navigation

from:
  - url: direct access to /ssrForm/page

to:
  - success page: valid submit (same /ssrForm/form route, no distinct URL)

## automated suite

`./mill formsScenarios.jvm.e2e` runs a Cucumber+Playwright suite mirroring
this page's journey (feature file:
`forms/scenarios/jvm/src/e2e/resources/features/ssr-form.feature`). It
starts the server in-process on port 8392 — prefer extending it over
re-verifying manually.

## gotchas (each one was a real bug once)

- Use Czech diacritics in at least one text value — pins the UTF-8
  charset path (browsers fall back to Latin-1 without it).
- Always verify remove-one-of-two-rows keeps the other row's values —
  pins multi-value field decoding (duplicate names must not merge).
- Type into a text field and then trigger a re-render via the select;
  the typed value must survive — pins the swap-race trigger narrowing.
- The form must carry `novalidate`; without it htmx silently drops all
  requests while any required field is blank (htmx:validation:halted).

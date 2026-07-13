# PURPOSE: Browser-level parity suite for the Datastar inquiry form (patch-elements SSE loop)
# PURPOSE: Mirrors ssr-form.feature step for step — same wording, the transport is the only variable
Feature: Datastar inquiry form
  The Datastar-rendered inquiry form proves the UIForm SSR loop rides
  Datastar actions the same way it rides HTMX: change-triggered morphs,
  server-side Required validation, repeated row add/remove, and faithful
  submission through the unchanged __submit protocol.

  Background:
    Given the Datastar inquiry form is open

  Scenario: Choosing order reveals the delivery section without leaving the page
    When I choose "order" as the request kind
    Then the delivery address field is visible
    And I am still on the form page
    And the summary reads "You are requesting a order with 1 item(s)."

  Scenario: Submitting a blank form shows human-readable required errors
    When I submit the form
    Then I see a validation error "Please fill in Name"
    And I see a validation error "Please fill in E-mail"
    And I see a validation error "Please fill in Quantity"
    And I see a validation error "Please fill in Description"

  Scenario: Typed text survives a change-triggered re-render
    When I fill in "Name" with "Michal Příhoda"
    And I choose "order" as the request kind
    Then the delivery address field is visible
    And the field "Name" contains "Michal Příhoda"

  Scenario: Removing one row keeps the other row's data
    When I add an item row
    Then the form has 2 item rows
    When I fill item row 2 with quantity "7" and description "Gadget"
    And I remove item row 1
    Then the form has 1 item row
    And item row 1 has quantity "7" and description "Gadget"

  Scenario: An invalid e-mail is rejected with a format error
    When I fill in "E-mail" with "not-an-email"
    And I submit the form
    Then I see a validation error "E-mail is not a valid e-mail address"

  Scenario: A valid order submits faithfully including Czech input
    When I choose "order" as the request kind
    And I fill in "Name" with "Michal Příhoda"
    And I fill in "E-mail" with "michal@example.com"
    And I fill in "Address" with "Brno, Česká 12"
    And I fill item row 1 with quantity "3" and description "Šroubovák"
    And I submit the form
    Then the inquiry is received
    And the received data includes "Michal Příhoda"
    And the received data includes "Brno, Česká 12"
    And the received data includes "Šroubovák"

  Scenario: Hidden token, checkbox, date and note all submit faithfully
    When I fill in "Name" with "Michal Příhoda"
    And I fill in "E-mail" with "michal@example.com"
    And I fill in "Note" with "Prosím o rychlé vyřízení"
    And I mark the inquiry as urgent
    And I set the deadline to "2026-08-01"
    And I fill item row 1 with quantity "3" and description "Šroubovák"
    And I submit the form
    Then the inquiry is received
    And the received "inquiry.token" is "proof"
    And the received "inquiry.request.urgent" is "true"
    And the received "inquiry.request.deadline" is "2026-08-01"
    And the received "inquiry.customer.note" is "Prosím o rychlé vyřízení"

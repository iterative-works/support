# PURPOSE: Browser-level regression suite for the SSR inquiry form (UIFormHtmlRenderer GET/POST loop)
# PURPOSE: Each scenario pins a behavior that route-level tests cannot observe (htmx, charset, swap races)
Feature: SSR inquiry form
  The server-rendered inquiry form proves the UIForm SSR loop:
  HTMX change-triggered re-renders, server-side Required validation,
  repeated row add/remove, and faithful submission.

  Background:
    Given the inquiry form is open

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

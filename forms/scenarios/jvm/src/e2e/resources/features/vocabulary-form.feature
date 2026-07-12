Feature: Vocabulary form conformance
  One declaration carrying every field kind, a registry-bound rule and every
  button intent renders and validates alike on the SSR page and in the SPA
  custom element. The control table is the pinned dispatch of both variants
  and must stay identical.

  Scenario: The SSR page renders every field kind as its pinned control
    Given the SSR vocabulary form is open
    Then the vocabulary renders these controls:
      | string                    | input:text     |
      | hidden                    | input:hidden   |
      | date                      | input:date     |
      | prose                     | textarea       |
      | select                    | input:text     |
      | checkbox                  | input:checkbox |
      | number                    | input:number   |
      | number_natural            | input:number   |
      | number_real_non_negative  | input:number   |
      | base_email                | input:email    |
      | base_phone                | input:tel      |
      | base_zip                  | input:text     |
      | base_country              | input:text     |
      | base_ruian                | input:text     |
      | tel                       | input:tel      |
      | acme_widget               | input:text     |

  Scenario: The SPA element renders every field kind as its pinned control
    # One deliberate divergence from the SSR table: a checkbox-typed Field stays a
    # text input in the SPA — checkbox inputs cannot carry the text value controller,
    # and bool Enums (not checkbox Fields) are the working checkbox story
    Given the SPA vocabulary form is open
    Then the vocabulary renders these controls:
      | string                    | input:text     |
      | hidden                    | input:hidden   |
      | date                      | input:date     |
      | prose                     | textarea       |
      | select                    | input:text     |
      | checkbox                  | input:text     |
      | number                    | input:number   |
      | number_natural            | input:number   |
      | number_real_non_negative  | input:number   |
      | base_email                | input:email    |
      | base_phone                | input:tel      |
      | base_zip                  | input:text     |
      | base_country              | input:text     |
      | base_ruian                | input:text     |
      | tel                       | input:tel      |
      | acme_widget               | input:text     |

  Scenario: The registered rule rejects an odd value on the SSR page
    Given the SSR vocabulary form is open
    When I fill in "Even number" with "3"
    And I submit the vocabulary form
    Then I see a validation error "must be even"

  Scenario: The registered rule rejects an odd value in the SPA element
    Given the SPA vocabulary form is open
    When I fill in "Even number" with "3"
    And I submit the vocabulary form
    Then I see a validation error "must be even"

  Scenario: An even value submits from the SSR page
    Given the SSR vocabulary form is open
    When I fill in "Even number" with "4"
    And I submit the vocabulary form
    Then the vocabulary is received
    And the received "vocab.even" is "4"

  Scenario: An even value submits from the SPA element
    Given the SPA vocabulary form is open
    When I fill in "Even number" with "4"
    And I submit the vocabulary form
    Then the vocabulary is received
    And the received "vocab.even" is "4"

  Scenario: The declared Submit button owns submission on the SSR page
    Given the SSR vocabulary form is open
    Then the declared Submit button is the only submit control

  Scenario: Action buttons keep the SSR form on the page
    Given the SSR vocabulary form is open
    When I click the vocabulary button "Ping server"
    And I click the vocabulary button "Local action"
    Then the vocabulary form is still shown

  Scenario: Declared buttons in the SPA element wait for a client handler
    Given the SPA vocabulary form is open
    When I click the vocabulary button "Send vocabulary"
    And I click the vocabulary button "Ping server"
    And I click the vocabulary button "Local action"
    Then the vocabulary form is still shown

# Acceptance specification. Automated evidence: tests/integration/broker-flow.mjs
@US08 @US09 @US10
Feature: List the current user's notifications

  Scenario: Authorized or healthy request
    Given a signed-in user with a current plan has received a goal notification
    When the client requests GET /api/v1/notifications
    Then the response status is 200
    And the notification appears in that user's inbox

  Scenario: Boundary or access control
    Given the review environment is available
    When the request omits the access token
    Then the response status is 401
    And notifications are not disclosed

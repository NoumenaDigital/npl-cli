# AI Coding & Contribution Guidelines

These instructions guide the AI in generating and modifying code for this project. Adhering to these rules ensures
consistency, quality, and maintainability.

---

## Core Principles

- **Build and Test Instructions**: All instructions for building the project and running tests are located in the root
  `README.md` file.
- **Tests Must Pass**: Before finalizing any changes, ensure that all tests are passing.
- **Focused Changes**: Keep diffs as small and targeted as possible. Do not change or refactor code unrelated to the
  task at hand. Commit early and often.

---

## Testing Strategy

- **Test-Driven Development (TDD)**: Write tests for new functionality _first_. Run the tests to confirm they fail as
  expected, then write the implementation code to make them pass.
- **No Altering Existing Tests**: Do not disable, remove, or change pre-existing tests that are failing. Address the
  underlying issue instead.
- **Integration Test Style**: Emulate the style and approach found in
  `src/test/kotlin/com/noumenadigital/npl/cli/OpenapiCommandIT.kt` when writing new integration tests.
- **Avoid Mocking**: Prefer using real object instances and integration tests over mocking frameworks wherever possible.

---

## Coding Standards

- **Simplicity and Clarity**: Write code that is easy to understand. Avoid deeply nested structures and overly complex
  logic.
- **Meaningful Comments**: Avoid obvious or redundant comments (e.g., `// this is a variable`). Only add comments to
  explain the "why" behind a specific or complex implementation choice.

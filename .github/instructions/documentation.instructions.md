# GitHub Copilot Instructions: Documentation

These instructions especially apply to all `**/*.md` and `**/*.mdx` files in
[website/](../../website/) folder. It also applies to Javadoc in `**/*.java`
with some additional rules to apply to.

## General Advice

We follow the Associated Press (AP) Stylebook with one exception: we use the
serial (Oxford) comma for clarity in technical writing.

* **Dictionary**: Use Merriam-Webster (<https://www.merriam-webster.com/>) as
  the authoritative reference for spelling and word definitions.
* **Capitalization**: Follow AP Style title case rules:
  * Capitalize the first word, the last word, and all nouns, pronouns,
    adjectives, verbs, and adverbs
  * Lowercase articles (a, an, the), coordinating conjunctions (and, but, or,
    for, nor), and prepositions with fewer than five letters (in, to, of) unless
    they begin or end the title
* **Numbers**: Spell out numbers one through nine; use numerals for 10 and above
* **Dates**: Use the month-day-year format (December 31, 2023)
* **Acronyms**: Spell out acronyms on first reference with the acronym in
  parentheses, then use the acronym alone in subsequent references
* **Technical Terms**: Use consistent capitalization for technical terms (e.g.,
  JavaScript, API, Docker)

## Javadoc

In general, ensure to use Javadoc best-practices aligned with the current
Java version (see [Java Instructions](./java.instructions.md) for details).

Mentioning some more explicit ones:

* Prefer `{@code}` over `<code>`.

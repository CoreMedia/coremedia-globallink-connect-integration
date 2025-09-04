---
applyTo: "**/*.java,**/*.md,**/*.mdx"
---

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

## Structure & Organization

* **Use consistent heading hierarchy**: Follow H1 → H2 → H3 progression without
  skipping levels
* **Write scannable content**: Use bullet points, numbered lists, and short
  paragraphs

## Content Guidelines

* **Write clear, concise sentences**: Aim for 20-25 words per sentence maximum
* **Use active voice**: Prefer "Configure the setting" over "The setting should
  be configured"
* **Define technical terms**: Either inline or in a glossary section
* **Include examples**: Provide code samples, screenshots, or step-by-step
  instructions

## Markdown Best Practices

* **Use semantic markup**: `**bold**` for emphasis, `_italic_` for stress,
  `` `code` `` for inline code
* **Format links properly**: Use descriptive link text, avoid "click here" or
  bare URLs
* **Structure code blocks**: Always specify language for syntax highlighting

  ````markdown
  ```java
  public class Example {
      // code here
  }
  ```
  ````

* **Use consistent list formatting**: Either all bullets or all numbers within
  a section

## Javadoc

In general, ensure to use Javadoc best-practices aligned with the current
Java version (see [Java Instructions](./java.instructions.md) for details).

### Javadoc Specific Rules

* **Document all public methods**: Include `@param`, `@return`, `@throws` for
  public APIs
* **Use `{@link}` for cross-references**: Link to related classes and methods
* **Write complete sentences**: Start with capital letter, end with period
* **Use present tense**: "Returns the user name" not "Will return the user name"
* **Prefer `{@code}` over `<code>`**: Use `{@code}` for inline code examples
* **Include examples**: Use `{@code}` or `<pre>` blocks to show usage patterns

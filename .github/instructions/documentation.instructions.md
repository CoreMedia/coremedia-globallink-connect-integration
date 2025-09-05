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

* **Primary Reference:**
  [Oracle Javadoc Style Guide](https://www.oracle.com/technical-resources/articles/java/javadoc-tool.html)
* **Modern Features:**
  [JavaDoc Guide, Release 21](https://docs.oracle.com/en/java/javase/21/javadoc/javadoc-guide.pdf)

### Javadoc Specific Rules

* **Document all public methods**: Include `@param`, `@return`, `@throws` for
  public APIs
* **Use `{@link}` for cross-references**: Link to related classes and methods
* **Write complete sentences**: Start with a capital letter, end with period
* **Use present tense**: "Returns the username" not "Will return the username"
* **Target advanced programmers**: Be concise and avoid explaining basic Java
  concepts
* **Document runtime exceptions**: Include `@throws` for `RuntimeException`
  subclasses that may be thrown

### Code Examples and Formatting

* **Prefer `{@code}` over `<code>`**: Use `{@code}` for inline code examples
* **Use `{@snippet}` for code blocks**: For Java 21+, use `{@snippet}` instead
  of `<pre>{@code}` blocks for multi-line examples
* **Limit line length**: Prefer 80 characters, maximum 120 characters, exceed
  only for long URLs, for example.

### HTML and Structure

* **Use `<p>` sparingly**: Place `<p>` on empty lines to separate paragraphs,
  don't use XHTML-style `<p></p>` tags
* **Maintain consistent indentation**: Align asterisks vertically

```java
/**
 * Returns the user's display name formatted for the current locale.
 * <p>
 * This method handles null values by returning a default placeholder.
 * The formatting follows RFC 2822 conventions for display names.
 *
 * @param userId the unique identifier for the user
 * @param locale the target locale for formatting
 * @return the formatted display name, never {@code null}
 * @throws IllegalArgumentException if userId is negative
 * @throws UserNotFoundException if no user exists with the given ID
 * @since 2.1
 */
```

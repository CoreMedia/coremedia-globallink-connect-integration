---
applyTo: "**/*.md"
---

# GitHub Copilot Instructions: Markdown

These instructions are dedicated to Markdown files in this repository, applying
to all files matching `**/*.md`. These rules complement the
[documentation.instructions.md](./documentation.instructions.md) file, which
contains style and content guidelines.

## General Copilot Instruction

When working with Markdown files, assume that you have expert knowledge in
Markdown syntax, GitHub Flavored Markdown (GFM), and documentation best
practices.

## Markdown Syntax & Structure

* **Use ATX-style headers**:
  Use `#` for headers (ATX style) rather than underline style:
  ```markdown
  # Heading 1
  ## Heading 2
  ### Heading 3
  ```
* **Maintain heading hierarchy**:
  Don't skip heading levels (e.g., don't jump from `#` to `###`). Follow the
  proper hierarchy: H1 → H2 → H3.
* **Use blank lines appropriately**:
  - Add blank lines before and after headers
  - Add blank lines before and after code blocks, lists, and blockquotes
  - Separate paragraphs with a single blank line

## Links & References

* **Use reference-style links for repeated URLs**:
  When the same URL is used multiple times, use reference-style links:
  ```markdown
  [link text][reference]

  [reference]: https://example.com
  ```
* **Prefer descriptive link text**:
  Avoid "click here" or bare URLs. Use descriptive text:
  ```markdown
  See the [installation guide](link) for details.
  ```
* **Use angle brackets for automatic links**:
  For email addresses and URLs that should appear as-is:
  ```markdown
  <https://example.com>
  <email@example.com>
  ```

## Code & Syntax Highlighting

* **Always specify language for code blocks**:
  Use fenced code blocks with language specification:
  ````markdown
  ```java
  public class Example {
      // code here
  }
  ```
  ````
* **Use inline code for technical terms**:
  Wrap technical terms, file names, and code elements in backticks:
  ```markdown
  The `pom.xml` file contains Maven configuration.
  ```
* **Indent code blocks consistently**:
  Within lists, indent code blocks with 4 spaces or align with list content.

## Lists

* **Use consistent list markers**:
  - For unordered lists, consistently use `*`, `-`, or `+` (this project uses
    `*`)
  - For ordered lists, use `1.`, `2.`, `3.` (or `1.`, `1.`, `1.` for
    maintainability)
* **Indent nested lists properly**:
  Indent nested list items with 2 spaces:
  ```markdown
  * Item 1
    * Nested item 1.1
    * Nested item 1.2
  * Item 2
  ```
* **Use task lists for checklists**:
  For checklists, use GitHub Flavored Markdown task lists:
  ```markdown
  - [ ] Task to do
  - [x] Completed task
  ```

## Tables

* **Align table columns**:
  Use pipe alignment for better readability:
  ```markdown
  | Column 1 | Column 2 | Column 3 |
  |----------|----------|----------|
  | Value 1  | Value 2  | Value 3  |
  ```
* **Use alignment markers**:
  Specify column alignment in the separator row:
  - Left: `|:---------|`
  - Center: `|:--------:|`
  - Right: `|---------:|`

## Images & Badges

* **Use descriptive alt text**:
  Always provide meaningful alt text for images:
  ```markdown
  ![CoreMedia Labs Logo](path/to/image.png "CoreMedia Labs Logo Title")
  ```
* **Use shields.io for badges**:
  For status badges, use shields.io with consistent styling:
  ```markdown
  ![Badge](https://img.shields.io/badge/label-message-color?style=for-the-badge)
  ```

## Front Matter (for MDX/Docusaurus)

* **Use YAML front matter**:
  For Docusaurus documentation, use YAML front matter:
  ```markdown
  ---
  id: page-id
  title: Page Title
  sidebar_label: Short Label
  ---
  ```
* **Keep front matter minimal**:
  Only include necessary metadata in front matter.

## Comments

* **Use HTML comments for notes**:
  For comments that should not appear in rendered output:
  ```markdown
  <!-- This is a comment -->
  ```
* **Document update instructions**:
  Use comments to explain what needs to be updated:
  ```markdown
  <!--
    On Update review and adapt the following badges:
       * Version numbers
       * URLs
  -->
  ```

## Formatting Best Practices

* **Line length**:
  Prefer line lengths around 80-100 characters for better diff readability, but
  don't break URLs or tables.
* **Trailing whitespace**:
  Avoid trailing whitespace except when intentionally creating a line break
  (two spaces at the end of line).
* **File endings**:
  End files with a single newline character.

## Special Elements

* **Use blockquotes for callouts**:
  For important notes or warnings:
  ```markdown
  > **Note**: This is an important note.
  >
  > It can span multiple paragraphs.
  ```
* **Use horizontal rules sparingly**:
  Separate major sections with horizontal rules:
  ```markdown
  ---
  ```

## Markdown Linting

* **Follow markdownlint rules**:
  If the project uses markdownlint or similar tools, follow their rules.
* **Validate links**:
  Ensure all links are valid and point to existing resources.
* **Check for broken references**:
  Verify that reference-style links have corresponding definitions.

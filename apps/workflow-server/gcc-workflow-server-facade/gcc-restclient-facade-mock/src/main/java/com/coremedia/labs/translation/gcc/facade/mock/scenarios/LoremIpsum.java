package com.coremedia.labs.translation.gcc.facade.mock.scenarios;

import org.jspecify.annotations.NullMarked;

/**
 * Utility class providing Lorem Ipsum text for testing purposes.
 *
 * @since 2506.0.1-1
 */
@NullMarked
public enum LoremIpsum {
  ;
  /**
   * A long Lorem Ipsum text for testing purposes.
   */
  @SuppressWarnings("SpellCheckingInspection")
  private static final String LOREM_IPSUM = """
    Lorem ipsum dolor sit amet, consectetur adipisici elit, sed eiusmod tempor
    incidunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis
    nostrud exercitation ullamco laboris nisi ut aliquid ex ea commodi
    consequat. Quis aute iure reprehenderit in voluptate velit esse cillum
    dolore eu fugiat nulla pariatur. Excepteur sint obcaecat cupiditat non
    proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
    Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse
    molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero
    eros et accumsan et iusto odio dignissim qui blandit praesent luptatum
    zzril delenit augue duis dolore te feugait nulla facilisi. Lorem ipsum dolor
    sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod
    tincidunt ut laoreet dolore magna aliquam erat volutpat. Ut wisi enim ad
    minim veniam, quis nostrud exerci tation ullamcorper suscipit lobortis nisl
    ut aliquip ex ea commodo consequat. Duis autem vel eum iriure dolor in
    hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu
    feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui
    blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla
    facilisi. Nam liber tempor cum soluta nobis eleifend option congue nihil
    imperdiet doming id quod mazim placerat facer possim assum. Lorem ipsum
    dolor sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod
    tincidunt ut laoreet dolore magna aliquam erat volutpat. Ut wisi enim ad
    minim veniam, quis nostrud exerci tation ullamcorper suscipit lobortis nisl
    ut aliquip ex ea commodo consequat. Duis autem vel eum iriure dolor in
    hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu
    feugiat nulla facilisis. At vero eos et accusam et justo duo dolores et ea
    rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum
    dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr,
    sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam
    erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea
    rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum
    dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr,
    At accusam aliquyam diam diam dolore dolores duo eirmod eos erat, et nonumy
    sed tempor et et invidunt justo labore Stet clita ea et gubergren, kasd
    magna no rebum. sanctus sea sed takimata ut vero voluptua. est Lorem ipsum
    dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr,
    sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam
    erat. Consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut
    labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et
    accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea
    takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet,
    consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut
    labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et
    accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea
    takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet,
    consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut
    labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et
    accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea
    takimata sanctus est Lorem ipsum dolor sit amet.
    """.strip().replace('\n', ' ');

  /**
   * Get a substring of the Lorem Ipsum text with the specified length.
   *
   * @param length the desired length of the substring
   * @return a substring of the Lorem Ipsum text
   */
  public static String loremIpsum(int length) {
    if (length <= LOREM_IPSUM.length()) {
      return LOREM_IPSUM.substring(0, length);
    } else {
      StringBuilder sb = new StringBuilder(length);
      while (sb.length() < length) {
        sb.append(LOREM_IPSUM);
      }
      return sb.substring(0, length);
    }
  }
}

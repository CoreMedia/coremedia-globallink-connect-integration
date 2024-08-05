import { as } from "@jangaroo/runtime";
import { session, Content, ContentRepository, Struct } from "@coremedia/studio-client.cap-rest-client";
import ContentProperties from "@coremedia/studio-client.cap-rest-client/content/ContentProperties";
import Logger from "@coremedia/studio-client.client-core-impl/logging/Logger";
import RemoteBeanUtil from "@coremedia/studio-client.client-core/data/RemoteBeanUtil";

/**
 * Access to the settings for the translation services.
 *
 * Note, that this class only provides access to the global settings, not
 * site specific settings.
 */
export class TranslationServicesSettings {
  static #instance: TranslationServicesSettings = null;

  /**
   * Path where to search for appropriate settings documents.
   */
  static readonly MAIN_SETTINGS_PATH = "/Settings/Options/Settings";

  /**
   * The name of the settings. It either denotes the name of a settings document
   * or a path in which to search for settings documents.
   */
  static readonly SETTINGS_NAME = "Translation Services";

  /**
   * Type to store TranslationSettings with.
   */
  static readonly CMSETTINGS_TYPE = "CMSettings";

  /**
   * The name of the document level property holding the settings struct.
   */
  static readonly P_SETTINGS = "settings";
  /**
   * The name of the global link property in settings struct.
   */
  static readonly P_GLOBALLINK = "globalLink";

  /**
   * The name of the property in settings struct that denotes the day offset for
   * due dates.
   */
  static readonly P_DAY_OFFSET_FOR_DUE_DATE = "dayOffsetForDueDate";

  /**
   * The default offset to use, if no (or no valid) offset is set in the
   * settings. Should be greater than 0 (zero), as the due date should be
   * in the future.
   */
  static readonly DEFAULT_DAY_OFFSET_FOR_DUE_DATE = 30;

  #mainSettingsPath: Content | null | undefined = undefined;

  /**
   * Get content repository from session.
   */
  static #getRepository(): ContentRepository {
    return session._.getConnection().getContentRepository();
  }

  /**
   * Resolve the given path to either its content, `null`, if it is not existing
   * or `undefined`, if it cannot be resolved yet.
   *
   * @param path - path to resolve
   * @param folder - optional folder to search in
   */
  static #resolvePath(path: string, folder?: Content): Content | null | undefined {
    const repository = TranslationServicesSettings.#getRepository();
    return repository.getChild(path, null, folder);
  }

  /**
   * Resolve path to a content, that is granted to be accessible. Automatically,
   * also adds a tracked dependency to that content.
   *
   * @param path - path to resolve
   * @param folder - optional folder to search in
   */
  static #resolveAccessiblePath(path: string, folder?: Content): Content | null | undefined {
    const resolved = TranslationServicesSettings.#resolvePath(path, folder);
    if (resolved === undefined) {
      return undefined;
    }

    const accessible: boolean | undefined = RemoteBeanUtil.isAccessible(resolved);
    if (accessible === undefined) {
      return undefined;
    }
    if (accessible) {
      return resolved;
    }
    return null;
  }

  /**
   * Initialize the main settings path, if not already done.
   */
  #initMainSettingsPath(): void {
    if (!this.#mainSettingsPath) {
      this.#mainSettingsPath = TranslationServicesSettings.#resolveAccessiblePath(
        TranslationServicesSettings.MAIN_SETTINGS_PATH,
      );
      if (Logger.isDebugEnabled() && this.#mainSettingsPath) {
        Logger.debug(
          `Main settings path initialized to: ${this.#mainSettingsPath.getPath()} (${this.#mainSettingsPath})`,
        );
      }
    }
  }

  /**
   * Provides the main settings path, if it is accessible.
   */
  #resolveMainSettingsPath(): Content | null | undefined {
    this.#initMainSettingsPath();
    return this.#mainSettingsPath;
  }

  /**
   * Resolve the root of the translation services settings. This may either
   * be a document of the given name, or a folder, which again contains
   * other settings documents.
   */
  #resolveTranslationServicesSettingsRoot(): Content | null | undefined {
    const mainSettingsPath = this.#resolveMainSettingsPath();
    if (!mainSettingsPath) {
      return mainSettingsPath;
    }
    return TranslationServicesSettings.#resolveAccessiblePath(
      TranslationServicesSettings.SETTINGS_NAME,
      mainSettingsPath,
    );
  }

  /**
   * Finds relevant settings documents for translation services in a
   * deterministic order.
   */
  #findSettingsDocuments(): Content[] | undefined {
    const settingsRoot = this.#resolveTranslationServicesSettingsRoot();
    if (settingsRoot === undefined) {
      return undefined;
    }
    if (settingsRoot === null || settingsRoot.isDestroyed()) {
      Logger.debug(`No settings found for translation services: ${settingsRoot}`);
      return [];
    }
    const intermediateResult: Content[] = [];
    if (settingsRoot.isDocument()) {
      Logger.debug(`Settings represented as single document. Adding: ${settingsRoot.getPath()} (${settingsRoot})`);
      intermediateResult.push(settingsRoot);
    } else {
      // Adds contents sorted by name, so that we have a deterministic order.
      Logger.debug(
        `Settings represented as folder. Adding child documents of: ${settingsRoot.getPath()} (${settingsRoot})`,
      );
      intermediateResult.push(...settingsRoot.getChildDocuments());
    }
    // Return only those documents, that are of type CMSETTINGS_TYPE:
    const result = intermediateResult.filter((content) =>
      content?.getType()?.isSubtypeOf(TranslationServicesSettings.CMSETTINGS_TYPE),
    );
    if (Logger.isDebugEnabled()) {
      const resultCsv = result.map((content) => `${content.getPath()} (${content})`).join(", ");
      Logger.debug(`Found ${result.length} settings documents for translation services: ${resultCsv}`);
    }
    return result;
  }

  static #getSettingsStruct(settingsDocument: Content): Struct | null | undefined {
    const properties: ContentProperties | undefined = settingsDocument.getProperties();
    if (properties === undefined) {
      return undefined;
    }
    return as(properties.get(TranslationServicesSettings.P_SETTINGS), Struct);
  }
  /**
   * Retrieves the `globallink` struct from the given settings document, if
   * available.
   */
  static #getGlobalLink(settingsDocument: Content): Struct | null | undefined {
    const settings: Struct | undefined = TranslationServicesSettings.#getSettingsStruct(settingsDocument);
    if (settings === undefined) {
      return undefined;
    }
    return as(settings.get(TranslationServicesSettings.P_GLOBALLINK), Struct);
  }

  /**
   * Retrieves the day offset for due dates from the given settings document,
   * if available and if providing a suitable value (thus, a non-negative
   * number).
   */
  static #getDayOffsetForDueDate(settingsDocument: Content): number | null | undefined {
    const globallink: Struct | null | undefined = TranslationServicesSettings.#getGlobalLink(settingsDocument);
    if (globallink === undefined) {
      return undefined;
    }
    if (globallink === null) {
      return null;
    }
    const result = globallink.get(TranslationServicesSettings.P_DAY_OFFSET_FOR_DUE_DATE);
    if (typeof result !== "number" || result < 0) {
      Logger.debug(
        `Invalid day offset for due date in settings at: ${settingsDocument.getPath()}: ${result} (${typeof result})`,
      );
      return null;
    }
    return result;
  }

  getDayOffsetForDueDate(): number | undefined {
    const settingsDocuments = this.#findSettingsDocuments();
    if (settingsDocuments === undefined) {
      return undefined;
    }
    // Find first setting, that provides a number.
    for (const settingsDocument of settingsDocuments) {
      const dayOffset = TranslationServicesSettings.#getDayOffsetForDueDate(settingsDocument);
      if (typeof dayOffset === "number") {
        if (Logger.isDebugEnabled()) {
          Logger.debug(`Using day offset ${dayOffset} for due date from settings at: ${settingsDocument.getPath()}`);
        }
        return dayOffset;
      }
    }
    Logger.debug("No available and valid day offset found in settings. Using default.");
    return TranslationServicesSettings.DEFAULT_DAY_OFFSET_FOR_DUE_DATE;
  }

  static getInstance() {
    if (this.#instance === null) {
      this.#instance = new TranslationServicesSettings();
      this.#instance.#initMainSettingsPath();
    }
    return this.#instance;
  }
}

/**
 * Provides access to the settings for the translation services.
 */
export const translationServicesSettings = TranslationServicesSettings.getInstance();

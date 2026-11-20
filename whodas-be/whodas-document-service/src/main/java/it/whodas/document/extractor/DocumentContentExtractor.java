package it.whodas.document.extractor;

/**
 * Lettore di contenuto per un gruppo di formati. Le implementazioni si
 * occupano solo di portare il contenuto del file in {@link RawContent}: la
 * interpretazione dei valori e' compito di WhodasContentParser.
 */
public interface DocumentContentExtractor {

    boolean supports(DocumentFormat format);

    /**
     * @throws IllegalArgumentException se il file non e' leggibile (password,
     *         file corrotto, formato dichiarato incompatibile con il contenuto)
     */
    RawContent extract(byte[] content);
}
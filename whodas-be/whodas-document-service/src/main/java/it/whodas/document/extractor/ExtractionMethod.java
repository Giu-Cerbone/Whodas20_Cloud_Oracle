package it.whodas.document.extractor;

/**
 * Modalita' con cui il contenuto di un documento e' stato letto. Va in
 * risposta all'utente cosi' che sappia se i valori sono arrivati dai campi del
 * modulo, dal testo del file o dall'OCR di una scansione.
 */
public enum ExtractionMethod {

    /** Campi AcroForm letti direttamente dal PDF: e' il caso piu' affidabile. */
    ACROFORM,

    /** Testo estratto dal PDF senza passare dall'OCR. */
    PDF_TEXT,

    /** Testo riconosciuto dall'OCR su un PDF scansionato o su un'immagine. */
    OCR,

    /** Documento Word .docx. */
    DOCX,

    /** Documento Word .doc. */
    DOC,

    /** Foglio Excel .xlsx. */
    XLSX,

    /** Foglio Excel .xls. */
    XLS,

    /** Nessun contenuto utilizzabile. */
    NONE
}
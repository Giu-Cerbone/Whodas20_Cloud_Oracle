package it.whodas.document.service.pdf;

/**
 * Etichetta e nome del campo di un riga della testata del modello.
 *
 * @param label etichetta stampata a sinistra del campo
 * @param name  nome del campo AcroForm, stabile: e' il contratto con l'import
 * @param value valore precompilato, null se il campo resta vuoto
 */
public record PdfField(String label, String name, String value) {
}
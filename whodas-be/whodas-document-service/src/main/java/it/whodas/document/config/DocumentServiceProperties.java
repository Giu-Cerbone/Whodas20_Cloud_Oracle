package it.whodas.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parametri del document service: servizi interni da interrogare, motore OCR
 * e valori di default dell'import.
 */
@ConfigurationProperties(prefix = "whodas.document")
public class DocumentServiceProperties {

    private final Clients clients = new Clients();
    private final Ocr ocr = new Ocr();
    private final ImportDefaults importDefaults = new ImportDefaults();
    private final Template template = new Template();

    public Clients getClients() {
        return clients;
    }

    public Ocr getOcr() {
        return ocr;
    }

    public ImportDefaults getImportDefaults() {
        return importDefaults;
    }

    public Template getTemplate() {
        return template;
    }

    public static class Clients {

        private String patientUrl = "http://localhost:7002";
        private String questionnaireUrl = "http://localhost:7003";
        private String assessmentUrl = "http://localhost:7004";
        private int connectTimeoutMs = 3000;
        private int readTimeoutMs = 15000;

        public String getPatientUrl() {
            return patientUrl;
        }

        public void setPatientUrl(String patientUrl) {
            this.patientUrl = patientUrl;
        }

        public String getQuestionnaireUrl() {
            return questionnaireUrl;
        }

        public void setQuestionnaireUrl(String questionnaireUrl) {
            this.questionnaireUrl = questionnaireUrl;
        }

        public String getAssessmentUrl() {
            return assessmentUrl;
        }

        public void setAssessmentUrl(String assessmentUrl) {
            this.assessmentUrl = assessmentUrl;
        }

        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }
    }

    public static class Ocr {

        private boolean enabled = true;
        private String language = "ita+eng";
        private String tessdataPath = "C:/Program Files/Tesseract-OCR/tessdata";
        private int dpi = 300;
        private int maxPages = 20;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public String getTessdataPath() {
            return tessdataPath;
        }

        public void setTessdataPath(String tessdataPath) {
            this.tessdataPath = tessdataPath;
        }

        public int getDpi() {
            return dpi;
        }

        public void setDpi(int dpi) {
            this.dpi = dpi;
        }

        public int getMaxPages() {
            return maxPages;
        }

        public void setMaxPages(int maxPages) {
            this.maxPages = maxPages;
        }
    }

    public static class ImportDefaults {

        private boolean createPatientIfMissing = true;
        private boolean saveAssessment = true;
        private boolean submitAndScore = true;
        private int minAnswersToScore = 30;

        public boolean isCreatePatientIfMissing() {
            return createPatientIfMissing;
        }

        public void setCreatePatientIfMissing(boolean createPatientIfMissing) {
            this.createPatientIfMissing = createPatientIfMissing;
        }

        public boolean isSaveAssessment() {
            return saveAssessment;
        }

        public void setSaveAssessment(boolean saveAssessment) {
            this.saveAssessment = saveAssessment;
        }

        public boolean isSubmitAndScore() {
            return submitAndScore;
        }

        public void setSubmitAndScore(boolean submitAndScore) {
            this.submitAndScore = submitAndScore;
        }

        public int getMinAnswersToScore() {
            return minAnswersToScore;
        }

        public void setMinAnswersToScore(int minAnswersToScore) {
            this.minAnswersToScore = minAnswersToScore;
        }
    }

    public static class Template {

        private int itemsPerPage = 12;
        private boolean includeOfficialNumber = true;

        public int getItemsPerPage() {
            return itemsPerPage;
        }

        public void setItemsPerPage(int itemsPerPage) {
            this.itemsPerPage = itemsPerPage;
        }

        public boolean isIncludeOfficialNumber() {
            return includeOfficialNumber;
        }

        public void setIncludeOfficialNumber(boolean includeOfficialNumber) {
            this.includeOfficialNumber = includeOfficialNumber;
        }
    }
}

package it.whodas.common.domain;

public enum DomainType {
    COGNITION("Comprendere e comunicare", "Cognition", "Understanding and communicating", 6),
    MOBILITY("Mobilità", "Mobility", "Moving and getting around", 5),
    SELF_CARE("Cura di sé", "Self-care", "Hygiene, dressing, eating and staying alone", 4),
    GETTING_ALONG("Relazionarsi con le persone", "Getting along", "Interacting with other people", 5),
    LIFE_ACTIVITIES("Attività della vita quotidiana", "Life activities", "Domestic responsibilities, leisure, work and school", 8),
    PARTICIPATION("Partecipazione alla vita sociale", "Participation", "Joining in community activities", 8);

    private final String italianLabel;
    private final String englishLabel;
    private final String description;
    private final int totalItems;

    DomainType(String italianLabel, String englishLabel, String description, int totalItems) {
        this.italianLabel = italianLabel;
        this.englishLabel = englishLabel;
        this.description = description;
        this.totalItems = totalItems;
    }

    public String getItalianLabel() { return italianLabel; }
    public String getEnglishLabel() { return englishLabel; }
    public String getDescription() { return description; }
    public int getTotalItems() { return totalItems; }
}
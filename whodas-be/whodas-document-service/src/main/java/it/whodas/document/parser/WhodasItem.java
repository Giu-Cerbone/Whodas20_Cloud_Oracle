package it.whodas.document.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import it.whodas.common.domain.DomainType;

/**
 * Catalogo WHODAS 2.0 a 36 item usato dal document service.
 *
 * Ogni item porta:
 * - code: il codice del catalogo del questionnaire service (D1S1..D1S36);
 * - officialCode: la numerazione del questionario ufficiale WHO (D1.1..D6.8);
 * - shortText: il testo breve con cui l'item e' descritto in questionnaire_items;
 * - officialText: la formulazione completa del questionario ufficiale, quella
 *   che l'intervistatore ritrova stampata sul modello.
 *
 * Il catalogo e' duplicato qui di proposito: il document service deve poter
 * generare il modello e interpretare un documento anche se il questionnaire
 * service non e' raggiungibile.
 */
public enum WhodasItem {

    D1S1("D1.1", DomainType.COGNITION, "Concentrare la propria attenzione per almeno 10 minuti",
            "Concentrarsi nel fare qualcosa per 10 minuti"),
    D1S2("D1.2", DomainType.COGNITION, "Ricordarsi di fare cose importanti",
            "Ricordarsi di fare cose importanti"),
    D1S3("D1.3", DomainType.COGNITION, "Risolvere i problemi della vita quotidiana",
            "Analizzare e trovare soluzioni ai problemi della vita quotidiana"),
    D1S4("D1.4", DomainType.COGNITION, "Usare il computer o il telefono cellulare",
            "Imparare a fare cose nuove, come per esempio raggiungere un posto nuovo"),
    D1S5("D1.5", DomainType.COGNITION, "Prendersi cura del proprio aspetto fisico",
            "Capire quello che dice la gente"),
    D1S6("D1.6", DomainType.COGNITION, "Risolvere i problemi di comunicazione con altre persone",
            "Iniziare e portare avanti una conversazione"),
    D1S7("D2.1", DomainType.MOBILITY, "Alzarsi in piedi e stare in piedi",
            "Stare in piedi a lungo, come per 30 minuti"),
    D1S8("D2.2", DomainType.MOBILITY, "Camminare", "Alzarsi da una posizione seduta"),
    D1S9("D2.3", DomainType.MOBILITY, "Salire le scale", "Muoversi dentro casa"),
    D1S10("D2.4", DomainType.MOBILITY, "Spostarsi all'interno della propria casa", "Uscire di casa"),
    D1S11("D2.5", DomainType.MOBILITY, "Spostarsi all'esterno, camminare",
            "Camminare per una lunga distanza, come per un chilometro"),
    D1S12("D3.1", DomainType.SELF_CARE, "Lavarsi", "Lavarsi tutto il corpo"),
    D1S13("D3.2", DomainType.SELF_CARE, "Vestirsi", "Vestirsi"),
    D1S14("D3.3", DomainType.SELF_CARE, "Mangiare", "Mangiare"),
    D1S15("D3.4", DomainType.SELF_CARE, "Stare da solo/a", "Stare da solo per qualche giorno"),
    D1S16("D4.1", DomainType.GETTING_ALONG, "Svolgere compiti o attivita' insieme ad altre persone",
            "Interagire con le persone che non conosce"),
    D1S17("D4.2", DomainType.GETTING_ALONG, "Prendere parte ad attivita' sociali",
            "Mantenere un'amicizia"),
    D1S18("D4.3", DomainType.GETTING_ALONG, "Far parte di una famiglia",
            "Andare d'accordo con le persone con cui e' legato"),
    D1S19("D4.4", DomainType.GETTING_ALONG, "Stabilire e mantenere amicizie", "Fare nuove amicizie"),
    D1S20("D4.5", DomainType.GETTING_ALONG, "Farsi influenzare da altre persone", "Attivita' sessuale"),
    D1S21("D5.1", DomainType.LIFE_ACTIVITIES, "Svolgere le faccende domestiche",
            "Occuparsi della casa e della famiglia per quanto e' di sua responsabilita'"),
    D1S22("D5.2", DomainType.LIFE_ACTIVITIES, "Fare acquisti",
            "Svolgere bene le attivita' domestiche piu' importanti, tra quelle che spettano a lei"),
    D1S23("D5.3", DomainType.LIFE_ACTIVITIES, "Preparare i pasti",
            "Portare a termine tutte le attivita' domestiche che lei doveva svolgere"),
    D1S24("D5.4", DomainType.LIFE_ACTIVITIES, "Fare i lavori di casa",
            "Portare a termine tutte le attivita' domestiche che le spettano nei tempi per lei usuali"),
    D1S25("D5.5", DomainType.LIFE_ACTIVITIES, "Fare commissioni",
            "Svolgere le sue attivita' lavorative o scolastiche quotidiane"),
    D1S26("D5.6", DomainType.LIFE_ACTIVITIES, "Farsi aiutare a trovare lavoro",
            "Svolgere bene i compiti lavorativi/scolastici piu' importanti"),
    D1S27("D5.7", DomainType.LIFE_ACTIVITIES, "Svolgere attivita' di lavoro",
            "Portare a termine tutto quello che doveva fare"),
    D1S28("D5.8", DomainType.LIFE_ACTIVITIES, "Prendere parte ad attivita' di svago e tempo libero",
            "Portare a termine tutto quello che doveva fare nei tempi per lei usuali"),
    D1S29("D6.1", DomainType.PARTICIPATION, "Prendere parte alle attivita' della comunita'",
            "Prendere parte ad attivita' comunitarie, per esempio feste o attivita' religiose, come chiunque altro"),
    D1S30("D6.2", DomainType.PARTICIPATION, "Informarsi e utilizzare i social media",
            "Quanta difficolta' ha avuto a vivere con dignita' a causa di barriere o ostacoli nel mondo che la circonda"),
    D1S31("D6.3", DomainType.PARTICIPATION, "Essere membro di un'associazione o di un club",
            "Quanta difficolta' ha avuto a vivere con dignita' a causa degli atteggiamenti o delle azioni di altre persone"),
    D1S32("D6.4", DomainType.PARTICIPATION, "Prendere parte ad attivita' culturali e sportive",
            "Quanta difficolta' ha avuto a prendere parte ad attivita' culturali o sportive"),
    D1S33("D6.5", DomainType.PARTICIPATION, "Conservare le amicizie",
            "Quanto tempo ha dedicato alle sue condizioni di salute o alle loro conseguenze"),
    D1S34("D6.6", DomainType.PARTICIPATION, "Farsi aiutare dagli altri",
            "Quanto e' stato coinvolto emotivamente dalle sue condizioni di salute"),
    D1S35("D6.7", DomainType.PARTICIPATION, "Contribuire alle attivita' della comunita'",
            "Quanta difficolta' ha avuto la sua famiglia a causa dei suoi problemi di salute"),
    D1S36("D6.8", DomainType.PARTICIPATION, "Godersi appieno la vita",
            "Quanta difficolta' ha avuto nel fare da solo qualcosa per relax o per piacere");

    private static final Map<String, WhodasItem> BY_CODE = new LinkedHashMap<>();
    private static final Map<String, WhodasItem> BY_OFFICIAL_CODE = new LinkedHashMap<>();
    private static final Map<String, WhodasItem> BY_ORDER = new LinkedHashMap<>();

    static {
        int order = 0;
        for (WhodasItem item : values()) {
            BY_CODE.put(item.code().toUpperCase(Locale.ROOT), item);
            BY_OFFICIAL_CODE.put(item.officialCode().toUpperCase(Locale.ROOT), item);
            BY_ORDER.put(String.valueOf(++order), item);
        }
    }

    private final String officialCode;
    private final DomainType domain;
    private final String shortText;
    private final String officialText;

    WhodasItem(String officialCode, DomainType domain, String shortText, String officialText) {
        this.officialCode = officialCode;
        this.domain = domain;
        this.shortText = shortText;
        this.officialText = officialText;
    }

    /**
     * Accetta sia il codice interno (D1S7) sia quello ufficiale (D2.1) e lo
     * riporta al formato del catalogo.
     */
    public static WhodasItem resolve(String rawCode) {
        if (rawCode == null) {
            return null;
        }
        String cleaned = rawCode.trim().toUpperCase(Locale.ROOT).replace(" ", "").replace("_", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        WhodasItem byCode = BY_CODE.get(cleaned);
        if (byCode != null) {
            return byCode;
        }
        WhodasItem byOfficial = BY_OFFICIAL_CODE.get(cleaned);
        if (byOfficial != null) {
            return byOfficial;
        }
        // "D1.1" scritto come "D1-1" oppure "1.1" senza la lettera del dominio.
        String compact = cleaned.replace(".", "").replace("-", "");
        for (WhodasItem item : values()) {
            if (item.officialCode.replace(".", "").equals(compact) || item.name().replace("S", "").equals(compact)) {
                return item;
            }
        }
        return BY_ORDER.get(cleaned);
    }

    /**
     * Cerca l'item a cui appartiene una posizione numerica: gli item WHODAS
     * sono numerati in modo progressivo (1..36) su tutti i documenti.
     */
    public static WhodasItem resolveByOrder(int order) {
        return BY_ORDER.get(String.valueOf(order));
    }

    public static List<WhodasItem> all() {
        return new ArrayList<>(Arrays.asList(values()));
    }

    public String code() {
        return name();
    }

    public String officialCode() {
        return officialCode;
    }

    public DomainType domain() {
        return domain;
    }

    public String shortText() {
        return shortText;
    }

    public String officialText() {
        return officialText;
    }
}
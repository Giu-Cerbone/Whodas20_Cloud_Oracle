import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Runner SQL minimale via JDBC, perche' su questa macchina non e' installato psql.
 *
 * Uso:
 *   java -cp postgresql.jar SqlRunner.java <url> <user> <password> <file.sql> [--quiet]
 *
 * Lo script viene eseguito in una singola transazione: in caso di errore
 * viene annullata e l'uscita e' 1. Le righe SELECT vengono stampate come
 * tabella, cosi' le query di verifica in coda agli script sono visibili.
 */
public class SqlRunner {

    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            System.err.println("Uso: SqlRunner <url> <user> <password> <file.sql> [--quiet]");
            System.exit(2);
        }
        String url = args[0];
        String user = args[1];
        String password = args[2];
        Path script = Path.of(args[3]);
        boolean quiet = args.length > 4 && "--quiet".equals(args[4]);

        String sql = Files.readString(script, StandardCharsets.UTF_8);
        List<String> statements = split(sql);
        if (!quiet) {
            System.out.println(">> " + script.getFileName() + " (" + statements.size() + " statement)");
        }

        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", password);

        try (Connection conn = DriverManager.getConnection(url, props)) {
            conn.setAutoCommit(false);
            try (Statement st = conn.createStatement()) {
                for (String statement : statements) {
                    boolean hasResultSet = st.execute(statement);
                    if (hasResultSet) {
                        print(st.getResultSet());
                    } else if (!quiet && st.getUpdateCount() > 0) {
                        System.out.println("   OK  (" + st.getUpdateCount() + " righe)");
                    }
                }
            }
            conn.commit();
            if (!quiet) {
                System.out.println(">> COMMIT eseguito");
            }
        } catch (SQLException ex) {
            System.err.println("!! ERRORE SQL: " + ex.getMessage());
            System.exit(1);
        }
    }

    private static List<String> split(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingle = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (c == '\n') {
                    inLineComment = false;
                    current.append(c);
                }
                continue;
            }
            if (inBlockComment) {
                if (c == '*' && next == '/') {
                    inBlockComment = false;
                    i++;
                }
                continue;
            }
            if (!inSingle && c == '-' && next == '-') {
                inLineComment = true;
                continue;
            }
            if (!inSingle && c == '/' && next == '*') {
                inBlockComment = true;
                i++;
                continue;
            }
            if (c == '\'') {
                inSingle = !inSingle;
                current.append(c);
                continue;
            }
            if (c == ';' && !inSingle) {
                addIfNotBlank(statements, current);
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        addIfNotBlank(statements, current);
        return statements;
    }

    private static void addIfNotBlank(List<String> target, StringBuilder buffer) {
        String statement = buffer.toString().trim();
        if (!statement.isEmpty()) {
            target.add(statement);
        }
    }

    private static void print(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columns = meta.getColumnCount();

        String[] headers = new String[columns];
        for (int i = 1; i <= columns; i++) {
            headers[i - 1] = meta.getColumnLabel(i);
        }
        System.out.println("   " + String.join(" | ", headers));

        int rows = 0;
        while (rs.next()) {
            StringBuilder line = new StringBuilder("   ");
            for (int i = 1; i <= columns; i++) {
                if (i > 1) {
                    line.append(" | ");
                }
                String value = rs.getString(i);
                line.append(value == null ? "-" : value);
            }
            System.out.println(line);
            rows++;
        }
        if (rows == 0) {
            System.out.println("   (0 righe)");
        }
    }
}

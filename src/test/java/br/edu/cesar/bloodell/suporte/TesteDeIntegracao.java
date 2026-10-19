package br.edu.cesar.bloodell.suporte;

import java.util.List;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Container singleton: iniciado uma vez e compartilhado por todas as classes de
 * integração. Não usar {@code @Testcontainers}/{@code @Container} aqui: a extensão
 * do JUnit para o container ao fim de cada classe, enquanto o Spring reaproveita o
 * contexto em cache apontando para o banco que já foi encerrado.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class TesteDeIntegracao {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("bloodell_teste")
            .withUsername("bloodell")
            .withPassword("bloodell_teste")
            .withReuse(true);

    static {
        POSTGRES.start();
    }

    /**
     * Esvazia as tabelas de domínio (preserva o histórico do Flyway). Para classes
     * que dependem de banco vazio, já que o container é compartilhado.
     */
    protected static void limparBanco(JdbcTemplate jdbc) {
        List<String> tabelas = jdbc.queryForList("""
                SELECT tablename FROM pg_tables
                WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'
                """, String.class);
        jdbc.execute("TRUNCATE TABLE " + String.join(", ", tabelas) + " RESTART IDENTITY CASCADE");
    }
}

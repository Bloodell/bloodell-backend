package br.edu.cesar.bloodell.autenticacao.dominio;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String nome;
    @Column(nullable = false, unique = true, length = 160)
    private String email;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Perfil perfil;
    @JsonIgnore
    @Column(name = "senha_hash", length = 100)
    private String senhaHash;
    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    protected Usuario() { }

    public Usuario(String nome, String email, Perfil perfil, String senhaHash) {
        validarNome(nome);
        validarEmail(email);
        if (!hashBcryptValido(senhaHash)) {
            throw new IllegalArgumentException("Usuario exige hash BCrypt valido.");
        }
        this.nome = nome.trim();
        this.email = normalizarEmail(email);
        this.perfil = Objects.requireNonNull(perfil);
        this.senhaHash = senhaHash;
        this.criadoEm = OffsetDateTime.now();
    }

    /** Null significa campo omitido; o DTO de PATCH recusa null explicito. */
    public void atualizarCadastro(String nome, String email, Perfil perfil) {
        if (nome != null) {
            validarNome(nome);
            this.nome = nome.trim();
        }
        if (email != null) {
            validarEmail(email);
            this.email = normalizarEmail(email);
        }
        if (perfil != null) this.perfil = perfil;
    }

    private static void validarNome(String nome) {
        if (nome == null || nome.isBlank() || nome.trim().length() > 120) {
            throw new IllegalArgumentException("Nome de usuario invalido.");
        }
    }

    private static void validarEmail(String email) {
        if (email == null || email.isBlank() || email.trim().length() > 160) {
            throw new IllegalArgumentException("E-mail de usuario invalido.");
        }
    }

    public static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean hashBcryptValido(String hash) {
        return hash != null && hash.matches("\\$2[aby]\\$(0[4-9]|[12][0-9]|3[01])\\$[./A-Za-z0-9]{53}");
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public Perfil getPerfil() { return perfil; }
    @JsonIgnore
    public String getSenhaHash() { return senhaHash; }
}

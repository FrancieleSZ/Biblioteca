package org.example;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Properties;
import java.util.Scanner;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Biblioteca {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/Biblioteca";
    private static final String DB_USER = "postgres";
    // Segredos: lidos da variável de ambiente ou do arquivo config.properties (que NÃO vai para o Git)
    private static final String DB_PASSWORD = lerConfig("DB_PASSWORD");
    private static final String SENHA_GERAL = lerConfig("SENHA_GERAL");

    private static final int PBKDF2_ITERACOES = 120_000;
    private static final String ALFABETO_SENHA = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    private static Scanner scanner = new Scanner(System.in);

    /** Procura a chave primeiro nas variáveis de ambiente e depois no arquivo config.properties. */
    private static String lerConfig(String chave) {
        String valor = System.getenv(chave);
        if (valor != null && !valor.isEmpty()) {
            return valor;
        }
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("config.properties")) {
            props.load(in);
        } catch (IOException e) {
            return null; // arquivo não existe
        }
        return props.getProperty(chave);
    }

    public static Connection conectar() throws SQLException {
        if (DB_PASSWORD == null) {
            throw new SQLException("DB_PASSWORD não configurada. Crie o arquivo config.properties (veja config.properties.example).");
        }
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    /** Pede a senha geral e confere com a configurada. */
    private static boolean validarSenhaGeral() {
        if (SENHA_GERAL == null) {
            System.out.println("SENHA_GERAL não configurada. Crie o arquivo config.properties (veja config.properties.example).");
            return false;
        }
        System.out.print("Digite a senha geral de acesso: ");
        if (!scanner.nextLine().equals(SENHA_GERAL)) {
            System.out.println("Senha incorreta!");
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        menuInicial();
    }

    // ---------------------------------------------------------------
    // Senha: geração, hash e verificação
    // ---------------------------------------------------------------

    /** Gera uma senha aleatória de 8 caracteres (sem caracteres que se confundem, como 0/O e 1/l). */
    private static String gerarSenha() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(ALFABETO_SENHA.charAt(random.nextInt(ALFABETO_SENHA.length())));
        }
        return sb.toString();
    }

    private static byte[] pbkdf2(char[] senha, byte[] salt, int iteracoes) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(senha, salt, iteracoes, 256);
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    }

    /** Retorna "iteracoes:salt:hash" para guardar no banco. A senha em si nunca é gravada. */
    private static String hashSenha(String senha) throws Exception {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] hash = pbkdf2(senha.toCharArray(), salt, PBKDF2_ITERACOES);
        return PBKDF2_ITERACOES + ":"
                + Base64.getEncoder().encodeToString(salt) + ":"
                + Base64.getEncoder().encodeToString(hash);
    }

    private static boolean verificarSenha(String senhaDigitada, String armazenado) {
        try {
            String[] partes = armazenado.split(":");
            int iteracoes = Integer.parseInt(partes[0]);
            byte[] salt = Base64.getDecoder().decode(partes[1]);
            byte[] esperado = Base64.getDecoder().decode(partes[2]);
            byte[] calculado = pbkdf2(senhaDigitada.toCharArray(), salt, iteracoes);
            return MessageDigest.isEqual(esperado, calculado);
        } catch (Exception e) {
            return false;
        }
    }

    // ---------------------------------------------------------------
    // Menus
    // ---------------------------------------------------------------

    public static void menuInicial() {
        while (true) {
            System.out.println("\n--- ESCOLHA ---");
            System.out.println("Cliente [1]");
            System.out.println("Funcionário [2]");
            System.out.println("Cadastrar novo funcionário [3]");
            System.out.println("Excluir funcionário [4]");
            System.out.print("Digite o número: ");

            try {
                int menu3 = Integer.parseInt(scanner.nextLine());

                if (menu3 == 1) {
                    menuCliente();
                } else if (menu3 == 2) {
                    funcionario();
                } else if (menu3 == 3) {
                    funcionarioCadastro();
                } else if (menu3 == 4) {
                    funcionarioExcluir();
                } else {
                    System.out.println("Opção Inválida!");
                }
            } catch (NumberFormatException e) {
                System.out.println("Digite apenas números!");
            }
        }
    }

    public static void menuCliente() {
        System.out.println("\n--- MENU CLIENTE ---");
        System.out.println("Pesquisar livro [1]");
        System.out.println("Alugar livro [2]");
        System.out.println("Relatar funcionário [3]");
        System.out.print("Digite o número: ");

        try {
            int menu = Integer.parseInt(scanner.nextLine());

            if (menu == 1) {
                pesquisarLivro();
            } else if (menu == 2) {
                alugarLivro();
            } else if (menu == 3) {
                relatarFuncionario();
            } else {
                System.out.println("Número fora da lista!");
            }
        } catch (NumberFormatException e) {
            System.out.println("Digite apenas números!");
        }
    }

    public static void pesquisarLivro() {
        System.out.print("Digite o nome do livro: ");
        String nomeDoLivro = scanner.nextLine();
        System.out.print("Digite o nome do autor do livro: ");
        String autor = scanner.nextLine();

        String sql = "SELECT nome, autor, ano, genero, empresa, id FROM livros WHERE nome = ? AND autor = ?";

        try (Connection conn = conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nomeDoLivro);
            pstmt.setString(2, autor);

            try (ResultSet resultado = pstmt.executeQuery()) {
                if (resultado.next()) {
                    System.out.println("\n--- LIVRO ---");
                    System.out.println("Nome:    " + resultado.getString("nome"));
                    System.out.println("Autor:   " + resultado.getString("autor"));
                    System.out.println("Ano:     " + resultado.getInt("ano"));
                    System.out.println("Gênero:  " + resultado.getString("genero"));
                    System.out.println("Empresa: " + resultado.getString("empresa"));
                    System.out.println("ID:      " + resultado.getInt("id"));
                } else {
                    System.out.println("\nERRO: Nenhum livro encontrado com esse nome e autor.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Erro ao acessar o banco de dados: " + e.getMessage());
        }
    }

    public static void alugarLivro() {
        System.out.print("Digite o nome do livro: ");
        String livro = scanner.nextLine();
        System.out.print("Digite o código do livro: ");
        try {
            int cod = Integer.parseInt(scanner.nextLine());
            // Lógica de aluguel pendente
        } catch (NumberFormatException e) {
            System.out.println("Código inválido!");
        }
    }

    public static void relatarFuncionario() {
        // Lógica de relato pendente
    }

    public static void menuBibliotecario() {
        System.out.println("\n--- MENU BIBLIOTECÁRIO ---");
        System.out.println("Cadastrar novo livro [1]");
        System.out.println("Pesquisar livro [2]");
        System.out.println("Devolver livro [3]");
        System.out.println("Relatar livro [4]");
        System.out.print("Digite o número: ");

        try {
            int menu2 = Integer.parseInt(scanner.nextLine());
            // Lógica do menu do bibliotecário
        } catch (NumberFormatException e) {
            System.out.println("Digite apenas números!");
        }
    }

    // ---------------------------------------------------------------
    // Cadastro de funcionário: salva no banco e gera a senha dele
    // ---------------------------------------------------------------

    public static void funcionarioCadastro() {
        if (!validarSenhaGeral()) {
            return;
        }

        System.out.println("Acesso liberado!");
        System.out.print("Nome do funcionário: ");
        String nome = scanner.nextLine().trim();
        System.out.print("Data de nascimento (dd/MM/aaaa): ");
        String nascimentoTexto = scanner.nextLine().trim();
        System.out.print("CPF: ");
        String cpf = scanner.nextLine().trim();

        if (nome.isEmpty() || cpf.isEmpty()) {
            System.out.println("Nome e CPF são obrigatórios!");
            return;
        }

        LocalDate nascimento;
        try {
            nascimento = LocalDate.parse(nascimentoTexto, DateTimeFormatter.ofPattern("dd/MM/uuuu"));
        } catch (DateTimeParseException e) {
            System.out.println("Data inválida! Use o formato dd/MM/aaaa.");
            return;
        }

        String senhaGerada = gerarSenha();
        String sql = "INSERT INTO funcionarios (nome, data_nascimento, cpf, senha_hash) VALUES (?, ?, ?, ?)";

        try (Connection conn = conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nome);
            pstmt.setDate(2, Date.valueOf(nascimento));
            pstmt.setString(3, cpf);
            pstmt.setString(4, hashSenha(senhaGerada));
            pstmt.executeUpdate();

            System.out.println("\nFuncionário cadastrado com sucesso!");
            System.out.println("Nome:  " + nome);
            System.out.println("Senha: " + senhaGerada);
            System.out.println("Anote a senha agora: ela não será mostrada novamente.");

        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                System.out.println("ERRO: já existe um funcionário com esse CPF.");
            } else {
                System.out.println("Erro ao acessar o banco de dados: " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("Erro ao gerar a senha: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Exclusão de funcionário (pelo CPF, que é único)
    // ---------------------------------------------------------------

    public static void funcionarioExcluir() {
        if (!validarSenhaGeral()) {
            return;
        }

        System.out.print("CPF do funcionário a excluir: ");
        String cpf = scanner.nextLine().trim();

        try (Connection conn = conectar()) {

            String nome = null;
            try (PreparedStatement busca = conn.prepareStatement("SELECT nome FROM funcionarios WHERE cpf = ?")) {
                busca.setString(1, cpf);
                try (ResultSet rs = busca.executeQuery()) {
                    if (rs.next()) {
                        nome = rs.getString("nome");
                    }
                }
            }

            if (nome == null) {
                System.out.println("ERRO: nenhum funcionário encontrado com esse CPF.");
                return;
            }

            System.out.print("Excluir " + nome + "? Digite S para confirmar: ");
            if (!scanner.nextLine().trim().equalsIgnoreCase("S")) {
                System.out.println("Exclusão cancelada.");
                return;
            }

            try (PreparedStatement exclui = conn.prepareStatement("DELETE FROM funcionarios WHERE cpf = ?")) {
                exclui.setString(1, cpf);
                exclui.executeUpdate();
                System.out.println("Funcionário excluído com sucesso!");
            }

        } catch (SQLException e) {
            if ("23503".equals(e.getSQLState())) {
                System.out.println("ERRO: esse funcionário tem relatos registrados e não pode ser excluído.");
            } else {
                System.out.println("Erro ao acessar o banco de dados: " + e.getMessage());
            }
        }
    }

    // ---------------------------------------------------------------
    // Login do funcionário: confere nome + senha com o que está no banco
    // ---------------------------------------------------------------

    public static void funcionario() {
        System.out.println("\n--- SENHA DE ACESSO ---");
        System.out.print("Nome completo: ");
        String nomeFuncionario = scanner.nextLine().trim();
        System.out.print("Senha: ");
        String senha = scanner.nextLine();

        String sql = "SELECT senha_hash FROM funcionarios WHERE nome = ?";
        boolean autenticado = false;

        try (Connection conn = conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nomeFuncionario);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    if (verificarSenha(senha, rs.getString("senha_hash"))) {
                        autenticado = true;
                        break;
                    }
                }
            }

        } catch (SQLException e) {
            System.out.println("Erro ao acessar o banco de dados: " + e.getMessage());
            return;
        }

        if (autenticado) {
            menuBibliotecario();
        } else {
            System.out.println("Acesso inválido!");
        }
    }
}
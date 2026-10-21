# Biblioteca

Sistema de biblioteca em Java, executado pelo terminal, que usa PostgreSQL para guardar livros e funcionários.

## Funcionalidades

**Implementadas**

- Pesquisar livro por nome e autor.
- Cadastrar funcionário (protegido por senha geral). O sistema gera uma senha aleatória para o funcionário, mostrada uma única vez. No banco fica guardado apenas o hash da senha (PBKDF2).
- Login de funcionário com nome e senha conferidos no banco.
- Excluir funcionário pelo CPF (protegido por senha geral, com confirmação).

**Em desenvolvimento**

- Alugar livro
- Relatar funcionário e relatar livro
- Menu do bibliotecário: cadastrar livro, pesquisar, devolver

## Tecnologias

- Java (versão definida no `pom.xml`)
- Maven
- PostgreSQL
- Driver JDBC `org.postgresql:postgresql` 42.7.3

## Como rodar

### 1. Criar o banco de dados

No PostgreSQL, crie o banco:

```sql
CREATE DATABASE Biblioteca;
```

Depois conecte nele e execute o script `biblioteca.sql`, que cria as tabelas (`livros`, `funcionarios`, `alugueis`, `relatos`) e insere alguns livros de exemplo.

### 2. Configurar as senhas

As senhas não ficam no código. Copie o arquivo de exemplo e preencha com os seus valores:

```
config.properties.example  ->  config.properties
```

```properties
DB_PASSWORD=senha_do_seu_postgres
SENHA_GERAL=senha_para_cadastrar_e_excluir_funcionarios
```

O `config.properties` está no `.gitignore` e não vai para o repositório. Como alternativa, você pode definir `DB_PASSWORD` e `SENHA_GERAL` como variáveis de ambiente.

Se o seu banco não estiver em `localhost:5432` ou o usuário não for `postgres`, ajuste `DB_URL` e `DB_USER` no início do arquivo `Biblioteca.java`.

### 3. Executar

Abra o projeto no IntelliJ, aguarde o Maven baixar as dependências e execute o método `main` da classe `org.example.Biblioteca`.

O `config.properties` é lido a partir da pasta onde o programa é executado, então ele precisa estar na raiz do projeto (junto do `pom.xml`).

## Estrutura do banco

| Tabela | Descrição |
|---|---|
| `livros` | Nome, autor, ano, gênero, editora e disponibilidade |
| `funcionarios` | Nome, data de nascimento, CPF (único) e hash da senha |
| `alugueis` | Livro alugado, cliente e datas de aluguel e devolução |
| `relatos` | Relatos sobre funcionários ou livros |

## Segurança

- Senhas de funcionários são guardadas como hash com salt, nunca em texto puro.
- As senhas do banco e a senha geral ficam fora do código e fora do Git.
- Todas as consultas usam `PreparedStatement`, o que protege contra SQL injection.

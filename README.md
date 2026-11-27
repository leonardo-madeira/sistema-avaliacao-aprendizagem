# Avalia: sistema de avaliação de aprendizagem

Trabalho 1 de Banco de Dados I, CDIA, Universidade Estadual de Londrina. Professor: Dr. Anderson Paulo Avila Santos.

**Integrantes:** Leonardo Madeira Alves Pereira e Gabriel Antônio Fabian Dors.

## Natureza e particularidade

O sistema permite cadastrar questões, montar provas e simulados, indicar alunos, registrar respostas e consultar resultados. Cada questão tem um valor específico dentro de cada avaliação. As objetivas são corrigidas automaticamente; as respostas de texto recebem nota do professor. O total por aluno é calculado a partir das notas de cada item.

Uma questão pode ser reutilizada em avaliações com valores diferentes. Cada aluno possui uma participação por avaliação, pode salvar e retomar respostas dentro do prazo e entrega sua tentativa para correção. Questões em branco valem zero. Uma correção textual pendente mantém o total final indisponível até a conclusão da correção.

## Funcionalidades

| Requisito | Implementação |
|---|---|
| RF1 | Cadastro de questões de múltipla escolha, com alternativas e gabarito, e de texto livre |
| RF2 | Avaliações com título, descrição, início, fim e situação de rascunho ou publicada |
| RF3 | Seleção e ordenação de questões existentes, com reutilização entre avaliações |
| RF4 | Seleção dos alunos participantes de cada avaliação |
| RF5 | Identificação por login e registro das respostas, com salvamento, retomada e entrega |
| RF6 | Valor por item, correção automática ou manual e total por aluno |
| RF7 | Três relatórios, cada um com tabela, gráfico e filtros |

O professor gerencia suas próprias avaliações, corrige textos e consulta os relatórios. O aluno acessa somente as avaliações destinadas a ele e os próprios resultados. A autorização é verificada pelo backend, incluindo bloqueio de alunos nos endpoints de relatórios. Avaliações publicadas ficam bloqueadas para edição.

## Tecnologias e organização

Java 21, Spring Boot 4.1.1, Maven Wrapper e PostgreSQL. O acesso ao banco usa DAOs JDBC com `JdbcTemplate` e `NamedParameterJdbcTemplate`, sem JPA ou Hibernate. O frontend usa HTML, CSS e JavaScript e é servido pela própria aplicação Java.

```text
backend/src/main/java/br/com/avaliacao/
├── api/              Controllers, DTOs e tratamento de erros
├── application/      Casos de uso e transações
├── domain/           Regras de negócio e contratos de persistência
└── infrastructure/   DAOs JDBC, segurança e configuração
frontend/             Telas, estilos e chamadas HTTP
database/             Schema, carga e consultas dos relatórios
docs/                 Modelagem e imagens
```

As gravações que envolvem várias tabelas são transacionais. SQL fica na infraestrutura de persistência; as consultas analíticas ficam em `database/relatorios.sql`, carregado como recurso pelos DAOs. Controllers e telas não acessam o banco diretamente.

## Modelo de dados

- [DER em imagem](docs/DER.png), com [versão SVG](docs/DER.svg) e [legenda](docs/DER.md).
- [Modelo Entidade Relacionamento](docs/MER.md).
- [Esquema relacional, chaves e justificativa de 3FN](docs/esquema.md).
- [DDL PostgreSQL](database/schema.sql).

Oito tabelas representam usuários, tipos de questão, questões, alternativas, avaliações, itens das avaliações, participações e respostas. O valor pertence a `avaliacao_questoes`, e a nota obtida pertence a `respostas`. A view `resultados_participacoes` calcula os totais e as pendências sem duplicar o total em uma coluna de participação.

## Como executar localmente

Os comandos abaixo usam Bash em Linux ou macOS. São necessários Git, JDK 21 e os clientes PostgreSQL (`psql` e `pg_isready`). Para o banco, usar Docker com daemon ativo ou uma instância PostgreSQL já instalada. A validação do projeto foi feita com PostgreSQL 17. O Maven Wrapper baixa o Maven e as dependências na primeira execução; não é necessário instalar Maven ou Node separadamente.

### 1. Clonar o repositório

```bash
git clone https://github.com/leonardo-madeira/sistema-avaliacao-aprendizagem.git
cd sistema-avaliacao-aprendizagem
java -version
```

Confirmar que o Java utilizado é o JDK 21. Se houver mais de uma versão instalada, ajustar `JAVA_HOME` e `PATH` para esse JDK.

### 2. Configurar a conexão

No mesmo terminal, escolher uma senha para o banco:

```bash
export DB_USER=avaliacao
read -r -s -p 'Senha do banco: ' DB_PASSWORD
printf '\n'
export DB_PASSWORD
export DB_URL=jdbc:postgresql://127.0.0.1:5432/avaliacao
export PGHOST=127.0.0.1
export PGPORT=5432
export PGUSER="$DB_USER"
export PGDATABASE=avaliacao
export PGPASSWORD="$DB_PASSWORD"
```

A senha do banco é independente da senha de login da aplicação. As variáveis precisam estar exportadas no terminal que iniciará o Java. A aplicação não carrega arquivos `.env` automaticamente.

### 3. Criar e iniciar um banco vazio

Com Docker, executar uma única vez para criar a instância:

```bash
docker volume create avalia-postgres-data
docker run --name avalia-postgres \
  -e POSTGRES_USER="$DB_USER" \
  -e POSTGRES_PASSWORD="$DB_PASSWORD" \
  -e POSTGRES_DB="$PGDATABASE" \
  -p 127.0.0.1:5432:5432 \
  -v avalia-postgres-data:/var/lib/postgresql/data \
  -d postgres:17
until pg_isready -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE"; do
  sleep 1
done
```

Nas próximas execuções, usar `docker start avalia-postgres`; o volume preserva os dados. Se já houver PostgreSQL na porta 5432, usar essa instância com um banco vazio de nome `avaliacao`, pertencente ao usuário configurado, ou escolher outra porta e ajustar `DB_URL`, `PGPORT` e o mapeamento do Docker. Em uma instalação nativa, criar o usuário e o banco com a ferramenta administrativa do PostgreSQL antes do próximo passo.

### 4. Executar schema e carga

Na raiz do repositório e sobre o banco vazio:

```bash
psql -v ON_ERROR_STOP=1 -f database/schema.sql
psql -v ON_ERROR_STOP=1 -f database/carga.sql
```

Executar nessa ordem, apenas na preparação inicial do banco. Os scripts não são migrações para reaplicar sobre dados existentes. O backend não cria tabelas nem carrega dados automaticamente.

A carga contém 22 usuários, sendo dois professores e vinte alunos, cinco avaliações, trinta questões, oitenta alternativas, cem participações e 608 respostas. Há avaliações sem entregas, tentativas em andamento, questões em branco e correções pendentes para demonstrar situações diferentes nos relatórios.

### 5. Configurar as contas de demonstração e iniciar

As contas da carga têm um marcador de senha ainda não configurada. Escolher uma senha de demonstração com pelo menos oito caracteres e no máximo 72 bytes:

```bash
read -r -s -p 'Senha de login para as contas de demonstracao: ' DEMO_PASSWORD
printf '\n'
export DEMO_PASSWORD
export CONFIGURAR_SENHAS_DEMO=true
cd backend
./mvnw spring-boot:run
```

A inicialização grava o hash da senha somente nas contas da carga que ainda têm o marcador. A mesma senha escolhida permite acessar essas contas. A configuração não redefine senhas que já foram inicializadas.

Abrir **http://127.0.0.1:8080/**. Não é necessário iniciar outro servidor para o frontend.

| Perfil | Email para entrar |
|---|---|
| Professora Clara | clara@example.test |
| Professor Rafael | rafael@example.test |
| Aluno de demonstração | aluno01@example.test |

Depois da primeira inicialização, parar com `Ctrl+C` e, nas próximas partidas, usar:

```bash
unset DEMO_PASSWORD CONFIGURAR_SENHAS_DEMO
./mvnw spring-boot:run
```

As senhas já gravadas continuam válidas. Ao abrir outro terminal, exportar novamente as variáveis de conexão do passo 2. Nenhuma senha ou token precisa ser colocado no código ou versionado.

### Alternativa: gerar o executável

Com as variáveis de conexão configuradas, dentro de `backend/`:

```bash
./mvnw -DskipTests package
java -jar target/avaliacao-backend-0.0.1-SNAPSHOT.jar
```

O JAR inclui o frontend e as consultas SQL dos relatórios. `SERVER_PORT` altera a porta HTTP, cujo padrão é 8080; `SERVER_ADDRESS` altera o endereço, cujo padrão é `127.0.0.1`.

## Percurso de demonstração

1. Entrar como professora e abrir o banco de questões. Cadastrar uma objetiva com gabarito e uma de texto livre.
2. Criar uma avaliação com início e fim que incluam o horário da demonstração, selecionar questões, atribuir valores e indicar o aluno `aluno01@example.test`.
3. Publicar a avaliação e sair. Entrar como aluno, responder e entregar a prova.
4. Voltar como professora, corrigir a resposta textual e consultar os relatórios.
5. Entrar como aluno para conferir a nota total após a correção.

A carga também permite abrir os relatórios imediatamente. Para realizar uma nova prova, criar uma avaliação no período atual, pois as datas da carga são fixas.

## Testes

Na pasta `backend/`, `./mvnw test` executa os testes de domínio. Os testes de integração são habilitados quando `TEST_DATABASE_URL` está definida.

Para integração, criar um banco **separado**, por exemplo `avaliacao_test`, com um usuário autorizado a criar e remover schemas, e executar:

```bash
export TEST_DATABASE_URL=jdbc:postgresql://127.0.0.1:5432/avaliacao_test
export TEST_DATABASE_USER="$DB_USER"
export TEST_DATABASE_PASSWORD="$DB_PASSWORD"
./mvnw test
```

Cada teste de integração prepara um schema isolado e o remove ao terminar. A validação realizada com PostgreSQL teve **72 testes aprovados**, cobrindo domínio, autenticação, autorização, cadastros, montagem, publicação, respostas, correção, transações e relatórios. As interfaces também foram verificadas em Chromium, em desktop e celular.

# Esquema relacional e normalização

Mapeamento do [DER](DER.md) para PostgreSQL. Todas as relações possuem `id BIGINT GENERATED ALWAYS AS IDENTITY` como chave primária. As associações têm identidade própria e restrições UNIQUE para preservar as chaves candidatas do domínio.

## Relações e atributos

PK indica chave primária, FK indica chave estrangeira e NULL identifica atributo opcional. Os demais atributos são NOT NULL. Tipos, valores padrão e restrições executáveis estão no [schema.sql](../database/schema.sql).

### usuarios

| Atributo | Tipo PostgreSQL | Restrições e referências |
|---|---|---|
| `id` | `BIGINT` | PK, identidade incremental; NOT NULL |
| `nome` | `VARCHAR(150)` | NOT NULL |
| `email` | `VARCHAR(254)` | NOT NULL |
| `senha_hash` | `TEXT` | NOT NULL |
| `perfil` | `VARCHAR(10)` | NOT NULL |
| `criado_em` | `TIMESTAMPTZ` | NOT NULL |

### tipos_questao

| Atributo | Tipo PostgreSQL | Restrições e referências |
|---|---|---|
| `id` | `BIGINT` | PK, identidade incremental; NOT NULL |
| `codigo` | `VARCHAR(20)` | NOT NULL |

### questoes

| Atributo | Tipo PostgreSQL | Restrições e referências |
|---|---|---|
| `id` | `BIGINT` | PK, identidade incremental; NOT NULL |
| `tipo_questao_id` | `BIGINT` | FK → tipos_questao.id; NOT NULL |
| `autor_id` | `BIGINT` | FK → usuarios.id; NOT NULL |
| `enunciado` | `TEXT` | NOT NULL |
| `criada_em` | `TIMESTAMPTZ` | NOT NULL |

### alternativas

| Atributo | Tipo PostgreSQL | Restrições e referências |
|---|---|---|
| `id` | `BIGINT` | PK, identidade incremental; NOT NULL |
| `questao_id` | `BIGINT` | FK → questoes.id; NOT NULL |
| `texto` | `TEXT` | NOT NULL |
| `ordem` | `SMALLINT` | NOT NULL |
| `correta` | `BOOLEAN` | NOT NULL |

### avaliacoes

| Atributo | Tipo PostgreSQL | Restrições e referências |
|---|---|---|
| `id` | `BIGINT` | PK, identidade incremental; NOT NULL |
| `professor_id` | `BIGINT` | FK → usuarios.id; NOT NULL |
| `titulo` | `VARCHAR(200)` | NOT NULL |
| `descricao` | `TEXT` | NOT NULL |
| `inicio_em` | `TIMESTAMPTZ` | NOT NULL |
| `fim_em` | `TIMESTAMPTZ` | NOT NULL |
| `status` | `VARCHAR(10)` | NOT NULL |
| `criada_em` | `TIMESTAMPTZ` | NOT NULL |

### avaliacao_questoes

| Atributo | Tipo PostgreSQL | Restrições e referências |
|---|---|---|
| `id` | `BIGINT` | PK, identidade incremental; NOT NULL |
| `avaliacao_id` | `BIGINT` | FK → avaliacoes.id; NOT NULL |
| `questao_id` | `BIGINT` | FK → questoes.id; NOT NULL |
| `ordem` | `SMALLINT` | NOT NULL |
| `valor` | `NUMERIC(7,2)` | NOT NULL |

### participacoes

| Atributo | Tipo PostgreSQL | Restrições e referências |
|---|---|---|
| `id` | `BIGINT` | PK, identidade incremental; NOT NULL |
| `avaliacao_id` | `BIGINT` | FK → avaliacoes.id; NOT NULL |
| `aluno_id` | `BIGINT` | FK → usuarios.id; NOT NULL |
| `iniciada_em` | `TIMESTAMPTZ` | NULL |
| `entregue_em` | `TIMESTAMPTZ` | NULL |

### respostas

| Atributo | Tipo PostgreSQL | Restrições e referências |
|---|---|---|
| `id` | `BIGINT` | PK, identidade incremental; NOT NULL |
| `participacao_id` | `BIGINT` | FK → participacoes.id; NOT NULL |
| `avaliacao_questao_id` | `BIGINT` | FK → avaliacao_questoes.id; NOT NULL |
| `alternativa_id` | `BIGINT` | FK → alternativas.id; NULL |
| `texto_resposta` | `TEXT` | NULL |
| `nota_obtida` | `NUMERIC(7,2)` | NULL |
| `respondida_em` | `TIMESTAMPTZ` | NOT NULL |

## Chaves candidatas e unicidade

| Relação | Outras chaves candidatas além de id |
|---|---|
| usuarios | email |
| tipos_questao | codigo |
| questoes | Nenhuma outra declarada |
| alternativas | (questao_id, ordem) |
| avaliacoes | Nenhuma outra declarada |
| avaliacao_questoes | (avaliacao_id, questao_id); (avaliacao_id, ordem) |
| participacoes | (avaliacao_id, aluno_id) |
| respostas | (participacao_id, avaliacao_questao_id) |

O índice parcial de alternativas permite no máximo uma alternativa correta por questão. Essa restrição condicional não é uma chave candidata da relação inteira.

## Mapeamento dos relacionamentos

Relacionamentos 1:N são implementados pela FK no lado N. A relação N:N entre Avaliação e Questão gera `avaliacao_questoes`, com ordem e valor específicos da questão naquela prova. A relação N:N entre aluno e Avaliação gera `participacoes`, com os momentos de início e entrega. `respostas` associa uma participação a um item da avaliação, com uma única resposta por par.

A FK opcional `respostas.alternativa_id` representa a alternativa selecionada em uma resposta objetiva. Para texto livre, ela é nula e `texto_resposta` contém o texto. CHECK e trigger exigem um conteúdo compatível com o tipo da questão e impedem alternativa de outra questão ou item de outra avaliação. A validação de perfil professor/aluno ocorre no backend.

## Justificativa de terceira forma normal

**O esquema está na 3FN porque os atributos são atômicos e cada atributo não primo depende integralmente de uma chave candidata de sua relação, sem dependências transitivas de atributos não chave.**

Na 1FN, alternativas, itens, participantes e respostas ficam em linhas próprias, sem listas de IDs ou grupos repetidos nas colunas. Na 2FN, as propriedades dependem da chave completa, inclusive nas chaves candidatas compostas: por exemplo, o valor depende do par avaliação/questão, e a nota obtida depende do par participação/item. A existência de um id substituto não substitui essa análise das chaves candidatas.

Na 3FN, nome e email do aluno ficam em `usuarios`, a descrição do tipo fica em `tipos_questao`, o enunciado fica em `questoes` e o valor fica em `avaliacao_questoes`. Essas informações não são repetidas em respostas ou participações. Reutilizar uma questão não duplica seu enunciado. A nota total é calculada pela view `resultados_participacoes`, sem coluna redundante de total em `participacoes`.

| Relação | Dependência funcional principal e decisão |
|---|---|
| usuarios | id → nome, email, senha_hash, perfil, criado_em; email também identifica a linha |
| tipos_questao | id → codigo; codigo também identifica a linha |
| questoes | id → tipo_questao_id, autor_id, enunciado, criada_em; dados do autor e do tipo ficam nas relações referenciadas |
| alternativas | id e (questao_id, ordem) determinam os demais atributos; cada alternativa ocupa uma linha |
| avaliacoes | id → professor_id, titulo, descricao, inicio_em, fim_em, status, criada_em; dados do professor ficam em usuarios |
| avaliacao_questoes | id e os dois pares únicos determinam os demais atributos; valor e ordem pertencem ao uso da questão na prova |
| participacoes | id e (avaliacao_id, aluno_id) determinam os demais atributos; não há nome do aluno ou título da prova repetidos |
| respostas | id e (participacao_id, avaliacao_questao_id) determinam os demais atributos; não há nota total, enunciado ou dados do aluno repetidos |

A nota objetiva armazenada é o resultado da correção daquela resposta; o trigger a calcula conforme o gabarito e o valor do item. Essa regra entre relações não cria uma dependência entre dois atributos não chave da mesma relação. Após publicação, a aplicação bloqueia mudanças na composição e pontuação da prova.

## Regras complementares

Questões de texto não têm alternativas. Objetivas completas possuem pelo menos duas opções e exatamente uma correta, validadas pela aplicação. Avaliações em rascunho podem não ter itens ou participantes; a publicação exige ambos. Questões sem resposta não geram linhas em `respostas`. Nota nula indica correção pendente; zero é uma nota atribuída. A nota deve ficar entre zero e o valor do item.

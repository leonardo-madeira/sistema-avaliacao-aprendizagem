# Diagrama Entidade Relacionamento

O modelo possui oito entidades e onze chaves estrangeiras, conforme [schema.sql](../database/schema.sql). A pontuação pertence ao item da avaliação; a nota obtida pertence à resposta.

![DER do sistema de avaliação de aprendizagem](DER.png)

[Versão vetorial SVG](DER.svg) · [Esquema relacional e 3FN](esquema.md) · [Modelo conceitual](MER.md)

## Legenda

PK é chave primária e FK é chave estrangeira. `?` indica atributo que aceita NULL. Em cada ligação, a cardinalidade fica junto à entidade correspondente: `1` significa exatamente um, `0..1` significa opcional e `0..N` significa zero ou muitos. Cada filho exige um pai, exceto Resposta → Alternativa, que é opcional para permitir respostas textuais.

O diagrama mostra as tabelas implementadas, com FKs visíveis. Os conceitos, nomes dos relacionamentos e regras de negócio estão no [MER](MER.md).

## Diagrama em Mermaid

```mermaid
erDiagram
    tipos_questao ||--o{ questoes : tipo_questao_id
    usuarios ||--o{ questoes : autor_id
    questoes ||--o{ alternativas : questao_id
    usuarios ||--o{ avaliacoes : professor_id
    avaliacoes ||--o{ avaliacao_questoes : avaliacao_id
    questoes ||--o{ avaliacao_questoes : questao_id
    avaliacoes ||--o{ participacoes : avaliacao_id
    usuarios ||--o{ participacoes : aluno_id
    participacoes ||--o{ respostas : participacao_id
    avaliacao_questoes ||--o{ respostas : avaliacao_questao_id
    alternativas |o--o{ respostas : alternativa_id
    usuarios {
        bigint id PK
        varchar nome
        varchar email
        text senha_hash
        varchar perfil
        timestamptz criado_em
    }
    tipos_questao {
        bigint id PK
        varchar codigo
    }
    questoes {
        bigint id PK
        bigint tipo_questao_id FK
        bigint autor_id FK
        text enunciado
        timestamptz criada_em
    }
    alternativas {
        bigint id PK
        bigint questao_id FK
        text texto
        smallint ordem
        boolean correta
    }
    avaliacoes {
        bigint id PK
        bigint professor_id FK
        varchar titulo
        text descricao
        timestamptz inicio_em
        timestamptz fim_em
        varchar status
        timestamptz criada_em
    }
    avaliacao_questoes {
        bigint id PK
        bigint avaliacao_id FK
        bigint questao_id FK
        smallint ordem
        numeric valor
    }
    participacoes {
        bigint id PK
        bigint avaliacao_id FK
        bigint aluno_id FK
        timestamptz iniciada_em "opcional"
        timestamptz entregue_em "opcional"
    }
    respostas {
        bigint id PK
        bigint participacao_id FK
        bigint avaliacao_questao_id FK
        bigint alternativa_id FK "opcional"
        text texto_resposta "opcional"
        numeric nota_obtida "opcional"
        timestamptz respondida_em
    }
```

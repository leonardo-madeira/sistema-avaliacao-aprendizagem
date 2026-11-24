BEGIN;

CREATE TABLE usuarios (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome VARCHAR(150) NOT NULL CHECK (btrim(nome) <> ''),
    email VARCHAR(254) NOT NULL CHECK (btrim(email) <> ''),
    senha_hash TEXT NOT NULL CHECK (btrim(senha_hash) <> ''),
    perfil VARCHAR(10) NOT NULL CHECK (perfil IN ('PROFESSOR', 'ALUNO')),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_usuarios_email UNIQUE (email),
    CONSTRAINT ck_usuarios_email_normalizado CHECK (email = lower(btrim(email)))
);

CREATE TABLE tipos_questao (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    CONSTRAINT ck_tipos_questao_codigo CHECK (codigo IN ('MULTIPLA_ESCOLHA', 'TEXTO_LIVRE'))
);

INSERT INTO tipos_questao (codigo) VALUES ('MULTIPLA_ESCOLHA'), ('TEXTO_LIVRE');

CREATE TABLE questoes (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo_questao_id BIGINT NOT NULL REFERENCES tipos_questao (id),
    autor_id BIGINT NOT NULL REFERENCES usuarios (id),
    enunciado TEXT NOT NULL CHECK (btrim(enunciado) <> ''),
    criada_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE alternativas (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    questao_id BIGINT NOT NULL REFERENCES questoes (id),
    texto TEXT NOT NULL CHECK (btrim(texto) <> ''),
    ordem SMALLINT NOT NULL CHECK (ordem > 0),
    correta BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_alternativas_ordem UNIQUE (questao_id, ordem)
);

CREATE UNIQUE INDEX uq_alternativas_correta ON alternativas (questao_id) WHERE correta;

CREATE TABLE avaliacoes (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    professor_id BIGINT NOT NULL REFERENCES usuarios (id),
    titulo VARCHAR(200) NOT NULL CHECK (btrim(titulo) <> ''),
    descricao TEXT NOT NULL DEFAULT '',
    inicio_em TIMESTAMPTZ NOT NULL,
    fim_em TIMESTAMPTZ NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'RASCUNHO',
    criada_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_avaliacoes_periodo CHECK (fim_em > inicio_em),
    CONSTRAINT ck_avaliacoes_status CHECK (status IN ('RASCUNHO', 'PUBLICADA'))
);

CREATE TABLE avaliacao_questoes (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    avaliacao_id BIGINT NOT NULL REFERENCES avaliacoes (id),
    questao_id BIGINT NOT NULL REFERENCES questoes (id),
    ordem SMALLINT NOT NULL CHECK (ordem > 0),
    valor NUMERIC(7,2) NOT NULL CHECK (valor > 0 AND valor <> 'NaN'::NUMERIC),
    CONSTRAINT uq_avaliacao_questoes_questao UNIQUE (avaliacao_id, questao_id),
    CONSTRAINT uq_avaliacao_questoes_ordem UNIQUE (avaliacao_id, ordem)
);

CREATE TABLE participacoes (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    avaliacao_id BIGINT NOT NULL REFERENCES avaliacoes (id),
    aluno_id BIGINT NOT NULL REFERENCES usuarios (id),
    iniciada_em TIMESTAMPTZ,
    entregue_em TIMESTAMPTZ,
    CONSTRAINT uq_participacoes_aluno UNIQUE (avaliacao_id, aluno_id),
    CONSTRAINT ck_participacoes_entrega CHECK (
        entregue_em IS NULL OR (iniciada_em IS NOT NULL AND entregue_em >= iniciada_em)
    )
);

CREATE TABLE respostas (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    participacao_id BIGINT NOT NULL REFERENCES participacoes (id),
    avaliacao_questao_id BIGINT NOT NULL REFERENCES avaliacao_questoes (id),
    alternativa_id BIGINT REFERENCES alternativas (id),
    texto_resposta TEXT,
    nota_obtida NUMERIC(7,2),
    respondida_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_respostas_questao UNIQUE (participacao_id, avaliacao_questao_id),
    CONSTRAINT ck_respostas_conteudo CHECK (
        (alternativa_id IS NOT NULL AND texto_resposta IS NULL)
        OR (alternativa_id IS NULL AND texto_resposta IS NOT NULL AND btrim(texto_resposta) <> '')
    ),
    CONSTRAINT ck_respostas_nota CHECK (nota_obtida >= 0 AND nota_obtida <> 'NaN'::NUMERIC)
);


CREATE FUNCTION validar_alternativa() RETURNS TRIGGER
LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM questoes q
        JOIN tipos_questao tq ON tq.id = q.tipo_questao_id
        WHERE q.id = NEW.questao_id AND tq.codigo = 'MULTIPLA_ESCOLHA'
    ) THEN
        RAISE EXCEPTION 'Alternativas somente sao permitidas em questoes de multipla escolha'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_alternativa
BEFORE INSERT OR UPDATE ON alternativas
FOR EACH ROW EXECUTE FUNCTION validar_alternativa();

CREATE FUNCTION validar_e_corrigir_resposta() RETURNS TRIGGER
LANGUAGE plpgsql AS $$
DECLARE
    prova_participante BIGINT;
    item RECORD;
    opcao RECORD;
BEGIN
    SELECT avaliacao_id INTO prova_participante
    FROM participacoes WHERE id = NEW.participacao_id;

    SELECT aq.avaliacao_id, aq.questao_id, aq.valor, tq.codigo INTO item
    FROM avaliacao_questoes aq
    JOIN questoes q ON q.id = aq.questao_id
    JOIN tipos_questao tq ON tq.id = q.tipo_questao_id
    WHERE aq.id = NEW.avaliacao_questao_id;

    IF prova_participante IS NULL OR item.avaliacao_id IS NULL
       OR prova_participante <> item.avaliacao_id THEN
        RAISE EXCEPTION 'A questao deve pertencer a avaliacao do participante'
            USING ERRCODE = '23514';
    END IF;

    IF item.codigo = 'MULTIPLA_ESCOLHA' THEN
        SELECT questao_id, correta INTO opcao
        FROM alternativas WHERE id = NEW.alternativa_id;

        IF opcao.questao_id IS NULL OR opcao.questao_id <> item.questao_id
           OR NEW.texto_resposta IS NOT NULL THEN
            RAISE EXCEPTION 'Selecione uma alternativa da propria questao'
                USING ERRCODE = '23514';
        END IF;

        NEW.nota_obtida := CASE WHEN opcao.correta THEN item.valor ELSE 0 END;
    ELSE
        IF NEW.alternativa_id IS NOT NULL OR NEW.texto_resposta IS NULL
           OR btrim(NEW.texto_resposta) = '' THEN
            RAISE EXCEPTION 'Uma questao de texto livre exige resposta textual'
                USING ERRCODE = '23514';
        END IF;

        IF TG_OP = 'UPDATE' THEN
            IF NEW.texto_resposta IS DISTINCT FROM OLD.texto_resposta
               OR NEW.participacao_id IS DISTINCT FROM OLD.participacao_id
               OR NEW.avaliacao_questao_id IS DISTINCT FROM OLD.avaliacao_questao_id THEN
                NEW.nota_obtida := NULL;
            END IF;
        END IF;
    END IF;

    IF NEW.nota_obtida IS NOT NULL AND (
        NEW.nota_obtida < 0 OR NEW.nota_obtida > item.valor OR NEW.nota_obtida = 'NaN'::NUMERIC
    ) THEN
        RAISE EXCEPTION 'A nota deve ficar entre zero e o valor da questao (%)', item.valor
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_e_corrigir_resposta
BEFORE INSERT OR UPDATE ON respostas
FOR EACH ROW EXECUTE FUNCTION validar_e_corrigir_resposta();

CREATE INDEX idx_questoes_tipo ON questoes (tipo_questao_id);
CREATE INDEX idx_questoes_autor ON questoes (autor_id);
CREATE INDEX idx_avaliacoes_professor ON avaliacoes (professor_id);
CREATE INDEX idx_avaliacoes_periodo ON avaliacoes (inicio_em);
CREATE INDEX idx_avaliacao_questoes_questao ON avaliacao_questoes (questao_id);
CREATE INDEX idx_participacoes_aluno ON participacoes (aluno_id);
CREATE INDEX idx_respostas_item ON respostas (avaliacao_questao_id);
CREATE INDEX idx_respostas_alternativa ON respostas (alternativa_id);

CREATE VIEW resultados_participacoes AS
SELECT p.id AS participacao_id,
       p.avaliacao_id,
       p.aluno_id,
       p.entregue_em,
       COALESCE(SUM(r.nota_obtida), 0) AS nota_parcial,
       CASE WHEN p.entregue_em IS NOT NULL
                 AND COUNT(r.id) FILTER (WHERE r.nota_obtida IS NULL) = 0
            THEN COALESCE(SUM(r.nota_obtida), 0)
            ELSE NULL
       END AS nota_total,
       COUNT(aq.id) AS quantidade_questoes,
       COUNT(r.id) AS quantidade_respondidas,
       COUNT(aq.id) FILTER (WHERE r.id IS NULL) AS quantidade_sem_resposta,
       COUNT(r.id) FILTER (WHERE r.nota_obtida IS NULL) AS quantidade_correcao_pendente,
       COALESCE(SUM(aq.valor), 0) AS valor_total
FROM participacoes p
LEFT JOIN avaliacao_questoes aq ON aq.avaliacao_id = p.avaliacao_id
LEFT JOIN respostas r ON r.participacao_id = p.id AND r.avaliacao_questao_id = aq.id
GROUP BY p.id;

COMMIT;

\set ON_ERROR_STOP on
\set professorId 1
\set inicio NULL
\set fim NULL
\set busca NULL
\set todasAvaliacoes true
\set avaliacaoIds NULL
\set notaMin 0
\set notaMax 100
\set minimoCorrigidas 0
\set minimoRespostas 1
\set limite 10

-- relatorio: desempenho
WITH filtradas AS (
    SELECT a.id, a.titulo, a.inicio_em
    FROM avaliacoes a
    WHERE a.professor_id = :professorId AND a.status = 'PUBLICADA'
      AND (CAST(:inicio AS DATE) IS NULL OR CAST(a.inicio_em AT TIME ZONE 'America/Sao_Paulo' AS DATE) >= CAST(:inicio AS DATE))
      AND (CAST(:fim AS DATE) IS NULL OR CAST(a.inicio_em AT TIME ZONE 'America/Sao_Paulo' AS DATE) <= CAST(:fim AS DATE))
      AND LOWER(a.titulo) LIKE '%' || LOWER(COALESCE(:busca, '')) || '%'
      AND (:todasAvaliacoes OR a.id IN (:avaliacaoIds))
), dados AS (
    SELECT p.id, p.avaliacao_id, p.entregue_em, rp.nota_total,
           100.0 * rp.nota_total / NULLIF(rp.valor_total, 0) AS percentual
    FROM participacoes p
    JOIN filtradas f ON f.id = p.avaliacao_id
    JOIN resultados_participacoes rp ON rp.participacao_id = p.id
), resumo AS (
    SELECT f.id AS avaliacao_id, f.titulo, f.inicio_em,
           (SELECT COALESCE(SUM(aq.valor), 0) FROM avaliacao_questoes aq WHERE aq.avaliacao_id = f.id) AS valor_total,
           COUNT(d.id) AS convidados,
           COUNT(d.id) FILTER (WHERE d.entregue_em IS NOT NULL) AS entregues,
           COUNT(d.id) FILTER (WHERE d.entregue_em IS NOT NULL AND d.nota_total IS NULL) AS aguardando_correcao,
           COUNT(d.id) FILTER (WHERE d.percentual BETWEEN :notaMin AND :notaMax) AS notas_consideradas,
           AVG(d.nota_total) FILTER (WHERE d.percentual BETWEEN :notaMin AND :notaMax) AS media_nota,
           AVG(d.percentual) FILTER (WHERE d.percentual BETWEEN :notaMin AND :notaMax) AS media_percentual,
           MIN(d.nota_total) FILTER (WHERE d.percentual BETWEEN :notaMin AND :notaMax) AS menor_nota,
           MAX(d.nota_total) FILTER (WHERE d.percentual BETWEEN :notaMin AND :notaMax) AS maior_nota
    FROM filtradas f LEFT JOIN dados d ON d.avaliacao_id = f.id
    GROUP BY f.id, f.titulo, f.inicio_em
    HAVING COUNT(d.id) FILTER (WHERE d.percentual BETWEEN :notaMin AND :notaMax) >= :minimoCorrigidas
)
SELECT resumo.*,
       (SELECT SUM(r.media_percentual * r.notas_consideradas) / NULLIF(SUM(r.notas_consideradas), 0)
        FROM resumo r) AS media_geral_percentual
FROM resumo ORDER BY inicio_em DESC, avaliacao_id DESC;

-- relatorio: questoes
WITH filtradas AS (
    SELECT a.id FROM avaliacoes a
    WHERE a.professor_id = :professorId AND a.status = 'PUBLICADA'
      AND (CAST(:inicio AS DATE) IS NULL OR CAST(a.inicio_em AT TIME ZONE 'America/Sao_Paulo' AS DATE) >= CAST(:inicio AS DATE))
      AND (CAST(:fim AS DATE) IS NULL OR CAST(a.inicio_em AT TIME ZONE 'America/Sao_Paulo' AS DATE) <= CAST(:fim AS DATE))
      AND LOWER(a.titulo) LIKE '%' || LOWER(COALESCE(:busca, '')) || '%'
      AND (:todasAvaliacoes OR a.id IN (:avaliacaoIds))
), contagens AS (
    SELECT q.id AS questao_id, q.enunciado, COUNT(p.id) AS oportunidades,
           COUNT(r.id) AS respondidas, COUNT(p.id) - COUNT(r.id) AS em_branco,
           SUM(CASE WHEN r.id IS NULL OR r.nota_obtida = 0 THEN 1 ELSE 0 END) AS erros
    FROM filtradas f JOIN avaliacao_questoes aq ON aq.avaliacao_id = f.id
    JOIN questoes q ON q.id = aq.questao_id
    JOIN tipos_questao tq ON tq.id = q.tipo_questao_id AND tq.codigo = 'MULTIPLA_ESCOLHA'
    JOIN participacoes p ON p.avaliacao_id = f.id AND p.entregue_em IS NOT NULL
    LEFT JOIN respostas r ON r.participacao_id = p.id AND r.avaliacao_questao_id = aq.id
    GROUP BY q.id, q.enunciado
    HAVING COUNT(p.id) >= :minimoRespostas
), taxas AS (
    SELECT contagens.*, 100.0 * erros / oportunidades AS percentual_erros FROM contagens
)
SELECT RANK() OVER (ORDER BY percentual_erros DESC) AS posicao, taxas.*
FROM taxas ORDER BY percentual_erros DESC, erros DESC, questao_id
FETCH FIRST :limite ROWS ONLY;

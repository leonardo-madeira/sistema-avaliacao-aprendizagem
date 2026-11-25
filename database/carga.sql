BEGIN;

INSERT INTO usuarios (nome, email, senha_hash, perfil) VALUES
    ('Professora Clara', 'clara@example.test', '!conta_demonstracao_sem_senha', 'PROFESSOR'),
    ('Professor Rafael', 'rafael@example.test', '!conta_demonstracao_sem_senha', 'PROFESSOR'),
    ('Ana Souza', 'aluno01@example.test', '!conta_demonstracao_sem_senha', 'ALUNO'),
    ('Bruno Lima', 'aluno02@example.test', '!conta_demonstracao_sem_senha', 'ALUNO'),
    ('Camila Oliveira', 'aluno03@example.test', '!conta_demonstracao_sem_senha', 'ALUNO'),
    ('Daniel Santos', 'aluno04@example.test', '!conta_demonstracao_sem_senha', 'ALUNO'),
    ('Eduarda Costa', 'aluno05@example.test', '!conta_demonstracao_sem_senha', 'ALUNO'),
    ('Felipe Almeida', 'aluno06@example.test', '!conta_demonstracao_sem_senha', 'ALUNO'),
    ('Gabriela Rocha', 'aluno07@example.test', '!conta_demonstracao_sem_senha', 'ALUNO'),
    ('Henrique Martins', 'aluno08@example.test', '!conta_demonstracao_sem_senha', 'ALUNO'),
    ('Isabela Ribeiro', 'aluno09@example.test', '!conta_demonstracao_sem_senha', 'ALUNO'),
    ('Joao Carvalho', 'aluno10@example.test', '!conta_demonstracao_sem_senha', 'ALUNO'),
    ('Julia Fernandes', 'aluno11@example.test', '!conta_demonstracao_sem_senha', 'ALUNO'),
    ('Lucas Pereira', 'aluno12@example.test', '!conta_demonstracao_sem_senha', 'ALUNO'),
    ('Mariana Gomes', 'aluno13@example.test', '!conta_demonstracao_sem_senha', 'ALUNO'),
    ('Nicolas Barbosa', 'aluno14@example.test', '!conta_demonstracao_sem_senha', 'ALUNO'),
    ('Olivia Nunes', 'aluno15@example.test', '!conta_demonstracao_sem_senha', 'ALUNO'),
    ('Pedro Melo', 'aluno16@example.test', '!conta_demonstracao_sem_senha', 'ALUNO'),
    ('Rafaela Teixeira', 'aluno17@example.test', '!conta_demonstracao_sem_senha', 'ALUNO'),
    ('Samuel Araujo', 'aluno18@example.test', '!conta_demonstracao_sem_senha', 'ALUNO'),
    ('Tatiana Dias', 'aluno19@example.test', '!conta_demonstracao_sem_senha', 'ALUNO'),
    ('Vitor Moreira', 'aluno20@example.test', '!conta_demonstracao_sem_senha', 'ALUNO');

INSERT INTO questoes (tipo_questao_id, autor_id, enunciado) VALUES
    (1, 1, 'Qual comando SQL cria uma tabela?'),
    (1, 1, 'Qual restricao identifica unicamente cada linha de uma tabela?'),
    (1, 1, 'Qual comando inclui novas linhas em uma tabela?'),
    (1, 1, 'Qual clausula filtra linhas antes do agrupamento?'),
    (1, 1, 'O que uma chave estrangeira estabelece?'),
    (1, 1, 'Qual consulta conta todas as linhas de usuarios?'),
    (1, 1, 'Qual comando altera valores de linhas existentes?'),
    (1, 1, 'Qual restricao proibe valores nulos?'),
    (1, 1, 'Qual juncao preserva todas as linhas da tabela da esquerda?'),
    (1, 1, 'Qual clausula filtra grupos apos uma agregacao?'),
    (1, 1, 'Qual funcao calcula a media de valores nao nulos?'),
    (1, 1, 'Qual comando confirma uma transacao?'),
    (1, 1, 'Qual comando desfaz as alteracoes de uma transacao nao confirmada?'),
    (1, 1, 'Qual clausula ordena o resultado de uma consulta?'),
    (1, 1, 'Qual operador testa se um valor pertence a uma lista?'),
    (1, 1, 'Qual predicado testa corretamente a ausencia de valor em email?'),
    (1, 1, 'O que uma restricao CHECK permite?'),
    (1, 1, 'Qual combinacao agrupa vendas por cliente?'),
    (1, 1, 'Qual palavra elimina linhas duplicadas do resultado?'),
    (1, 1, 'Qual operador encontra nomes iniciados por Ana?'),
    (2, 2, 'Explique a diferenca entre chave primaria e chave estrangeira.'),
    (2, 2, 'Explique a diferenca entre WHERE e HAVING.'),
    (2, 2, 'Descreva o objetivo da terceira forma normal.'),
    (2, 2, 'De um exemplo em que LEFT JOIN e necessario.'),
    (2, 2, 'Explique por que uma transferencia bancaria precisa de transacao.'),
    (2, 2, 'Explique a diferenca entre DELETE e DROP TABLE.'),
    (2, 2, 'Descreva como representar uma associacao muitos para muitos.'),
    (2, 2, 'Explique como o SQL trata NULL em comparacoes.'),
    (2, 2, 'Explique a importancia de uma restricao UNIQUE.'),
    (2, 2, 'Explique quando um indice pode melhorar uma consulta.');

INSERT INTO alternativas (questao_id, texto, ordem, correta) VALUES
    (1, 'CREATE TABLE', 1, TRUE),
    (1, 'INSERT INTO', 2, FALSE),
    (1, 'SELECT', 3, FALSE),
    (1, 'DROP VIEW', 4, FALSE),
    (2, 'CHECK', 1, FALSE),
    (2, 'PRIMARY KEY', 2, TRUE),
    (2, 'DEFAULT', 3, FALSE),
    (2, 'ORDER BY', 4, FALSE),
    (3, 'UPDATE', 1, FALSE),
    (3, 'DELETE', 2, FALSE),
    (3, 'INSERT INTO', 3, TRUE),
    (3, 'ALTER TABLE', 4, FALSE),
    (4, 'GROUP BY', 1, FALSE),
    (4, 'HAVING', 2, FALSE),
    (4, 'ORDER BY', 3, FALSE),
    (4, 'WHERE', 4, TRUE),
    (5, 'Uma referencia a uma chave de outra tabela', 1, TRUE),
    (5, 'Uma ordem de exibicao', 2, FALSE),
    (5, 'Uma copia completa de outra tabela', 3, FALSE),
    (5, 'Um valor sempre opcional', 4, FALSE),
    (6, 'SELECT SUM(*) FROM usuarios', 1, FALSE),
    (6, 'SELECT COUNT(*) FROM usuarios', 2, TRUE),
    (6, 'SELECT AVG(*) FROM usuarios', 3, FALSE),
    (6, 'SELECT TOTAL(*) FROM usuarios', 4, FALSE),
    (7, 'CREATE', 1, FALSE),
    (7, 'INSERT', 2, FALSE),
    (7, 'UPDATE', 3, TRUE),
    (7, 'GRANT', 4, FALSE),
    (8, 'UNIQUE', 1, FALSE),
    (8, 'DEFAULT', 2, FALSE),
    (8, 'CHECK', 3, FALSE),
    (8, 'NOT NULL', 4, TRUE),
    (9, 'LEFT JOIN', 1, TRUE),
    (9, 'INNER JOIN', 2, FALSE),
    (9, 'CROSS JOIN', 3, FALSE),
    (9, 'RIGHT JOIN', 4, FALSE),
    (10, 'WHERE', 1, FALSE),
    (10, 'HAVING', 2, TRUE),
    (10, 'LIMIT', 3, FALSE),
    (10, 'DISTINCT', 4, FALSE),
    (11, 'SUM', 1, FALSE),
    (11, 'COUNT', 2, FALSE),
    (11, 'AVG', 3, TRUE),
    (11, 'MAX', 4, FALSE),
    (12, 'ROLLBACK', 1, FALSE),
    (12, 'SAVEPOINT', 2, FALSE),
    (12, 'BEGIN', 3, FALSE),
    (12, 'COMMIT', 4, TRUE),
    (13, 'ROLLBACK', 1, TRUE),
    (13, 'COMMIT', 2, FALSE),
    (13, 'VACUUM', 3, FALSE),
    (13, 'ANALYZE', 4, FALSE),
    (14, 'WHERE', 1, FALSE),
    (14, 'ORDER BY', 2, TRUE),
    (14, 'GROUP BY', 3, FALSE),
    (14, 'HAVING', 4, FALSE),
    (15, 'LIKE', 1, FALSE),
    (15, 'BETWEEN', 2, FALSE),
    (15, 'IN', 3, TRUE),
    (15, 'IS NULL', 4, FALSE),
    (16, 'email = NULL', 1, FALSE),
    (16, 'email <> NULL', 2, FALSE),
    (16, 'email IN NULL', 3, FALSE),
    (16, 'email IS NULL', 4, TRUE),
    (17, 'Validar uma condicao sobre os valores da linha', 1, TRUE),
    (17, 'Consultar automaticamente qualquer tabela', 2, FALSE),
    (17, 'Criar uma senha', 3, FALSE),
    (17, 'Ordenar as linhas fisicamente', 4, FALSE),
    (18, 'ORDER BY cliente_id', 1, FALSE),
    (18, 'GROUP BY cliente_id', 2, TRUE),
    (18, 'WHERE cliente_id', 3, FALSE),
    (18, 'LIMIT cliente_id', 4, FALSE),
    (19, 'UNIQUE KEY', 1, FALSE),
    (19, 'PRIMARY KEY', 2, FALSE),
    (19, 'DISTINCT', 3, TRUE),
    (19, 'CHECK', 4, FALSE),
    (20, 'nome = ''Ana%''', 1, FALSE),
    (20, 'nome IN (''Ana%'')', 2, FALSE),
    (20, 'nome IS ''Ana%''', 3, FALSE),
    (20, 'nome LIKE ''Ana%''', 4, TRUE);

INSERT INTO avaliacoes (professor_id, titulo, descricao, inicio_em, fim_em, status) VALUES
    (1, 'Fundamentos de SQL', 'Comandos basicos e integridade.', '2026-10-30 19:15:00-03', '2026-10-30 21:00:00-03', 'PUBLICADA'),
    (2, 'Consultas e filtros', 'Selecao, ordenacao e agregacoes.', '2026-11-06 19:15:00-03', '2026-11-06 21:00:00-03', 'PUBLICADA'),
    (1, 'Relacionamentos e normalizacao', 'Chaves, juncoes e modelagem.', '2026-11-13 19:15:00-03', '2026-11-13 21:00:00-03', 'PUBLICADA'),
    (2, 'Transacoes e revisao', 'Revisao de SQL com transacoes.', '2026-11-20 19:15:00-03', '2026-11-20 21:00:00-03', 'PUBLICADA'),
    (1, 'Simulado final', 'Preparacao para a avaliacao final.', '2026-11-27 19:15:00-03', '2026-11-27 21:00:00-03', 'PUBLICADA');

INSERT INTO avaliacao_questoes (avaliacao_id, questao_id, ordem, valor) VALUES
    (1, 1, 1, 1),
    (1, 2, 2, 1),
    (1, 3, 3, 1),
    (1, 4, 4, 1),
    (1, 5, 5, 1),
    (1, 6, 6, 1),
    (1, 7, 7, 1),
    (1, 21, 8, 1),
    (1, 22, 9, 1),
    (1, 23, 10, 1),
    (2, 4, 1, 1),
    (2, 5, 2, 1),
    (2, 6, 3, 1),
    (2, 7, 4, 1),
    (2, 8, 5, 1),
    (2, 9, 6, 1),
    (2, 10, 7, 1),
    (2, 23, 8, 1),
    (2, 24, 9, 1),
    (2, 25, 10, 1),
    (3, 7, 1, 1),
    (3, 8, 2, 1),
    (3, 9, 3, 1),
    (3, 10, 4, 1),
    (3, 11, 5, 1),
    (3, 12, 6, 1),
    (3, 13, 7, 1),
    (3, 25, 8, 1),
    (3, 26, 9, 1),
    (3, 27, 10, 1),
    (4, 10, 1, 1),
    (4, 11, 2, 1),
    (4, 12, 3, 1),
    (4, 13, 4, 1),
    (4, 14, 5, 1),
    (4, 15, 6, 1),
    (4, 16, 7, 1),
    (4, 27, 8, 1),
    (4, 28, 9, 1),
    (4, 29, 10, 1),
    (5, 13, 1, 1),
    (5, 14, 2, 1),
    (5, 15, 3, 1),
    (5, 16, 4, 1),
    (5, 17, 5, 1),
    (5, 18, 6, 1),
    (5, 19, 7, 1),
    (5, 29, 8, 1),
    (5, 30, 9, 1),
    (5, 21, 10, 1);

INSERT INTO participacoes (avaliacao_id, aluno_id, iniciada_em, entregue_em) VALUES
    (1, 3, '2026-10-30 19:20:00-03', '2026-10-30 20:11:00-03'),
    (1, 4, '2026-10-30 19:20:00-03', '2026-10-30 20:12:00-03'),
    (1, 5, '2026-10-30 19:20:00-03', '2026-10-30 20:13:00-03'),
    (1, 6, '2026-10-30 19:20:00-03', '2026-10-30 20:14:00-03'),
    (1, 7, '2026-10-30 19:20:00-03', '2026-10-30 20:15:00-03'),
    (1, 8, '2026-10-30 19:20:00-03', '2026-10-30 20:16:00-03'),
    (1, 9, '2026-10-30 19:20:00-03', '2026-10-30 20:17:00-03'),
    (1, 10, '2026-10-30 19:20:00-03', '2026-10-30 20:18:00-03'),
    (1, 11, '2026-10-30 19:20:00-03', '2026-10-30 20:19:00-03'),
    (1, 12, '2026-10-30 19:20:00-03', '2026-10-30 20:20:00-03'),
    (1, 13, '2026-10-30 19:20:00-03', '2026-10-30 20:21:00-03'),
    (1, 14, '2026-10-30 19:20:00-03', '2026-10-30 20:22:00-03'),
    (1, 15, '2026-10-30 19:20:00-03', '2026-10-30 20:23:00-03'),
    (1, 16, '2026-10-30 19:20:00-03', '2026-10-30 20:24:00-03'),
    (1, 17, '2026-10-30 19:20:00-03', '2026-10-30 20:25:00-03'),
    (1, 18, '2026-10-30 19:20:00-03', '2026-10-30 20:26:00-03'),
    (1, 19, NULL, NULL),
    (1, 20, NULL, NULL),
    (1, 21, NULL, NULL),
    (1, 22, NULL, NULL),
    (2, 3, '2026-11-06 19:20:00-03', '2026-11-06 20:11:00-03'),
    (2, 4, '2026-11-06 19:20:00-03', '2026-11-06 20:12:00-03'),
    (2, 5, '2026-11-06 19:20:00-03', '2026-11-06 20:13:00-03'),
    (2, 6, '2026-11-06 19:20:00-03', '2026-11-06 20:14:00-03'),
    (2, 7, '2026-11-06 19:20:00-03', '2026-11-06 20:15:00-03'),
    (2, 8, '2026-11-06 19:20:00-03', '2026-11-06 20:16:00-03'),
    (2, 9, '2026-11-06 19:20:00-03', '2026-11-06 20:17:00-03'),
    (2, 10, '2026-11-06 19:20:00-03', '2026-11-06 20:18:00-03'),
    (2, 11, '2026-11-06 19:20:00-03', '2026-11-06 20:19:00-03'),
    (2, 12, '2026-11-06 19:20:00-03', '2026-11-06 20:20:00-03'),
    (2, 13, '2026-11-06 19:20:00-03', '2026-11-06 20:21:00-03'),
    (2, 14, '2026-11-06 19:20:00-03', '2026-11-06 20:22:00-03'),
    (2, 15, '2026-11-06 19:20:00-03', '2026-11-06 20:23:00-03'),
    (2, 16, '2026-11-06 19:20:00-03', '2026-11-06 20:24:00-03'),
    (2, 17, '2026-11-06 19:20:00-03', '2026-11-06 20:25:00-03'),
    (2, 18, '2026-11-06 19:20:00-03', '2026-11-06 20:26:00-03'),
    (2, 19, '2026-11-06 19:20:00-03', '2026-11-06 20:27:00-03'),
    (2, 20, '2026-11-06 19:20:00-03', '2026-11-06 20:28:00-03'),
    (2, 21, NULL, NULL),
    (2, 22, NULL, NULL),
    (3, 3, '2026-11-13 19:20:00-03', '2026-11-13 20:11:00-03'),
    (3, 4, '2026-11-13 19:20:00-03', '2026-11-13 20:12:00-03'),
    (3, 5, '2026-11-13 19:20:00-03', '2026-11-13 20:13:00-03'),
    (3, 6, '2026-11-13 19:20:00-03', '2026-11-13 20:14:00-03'),
    (3, 7, '2026-11-13 19:20:00-03', '2026-11-13 20:15:00-03'),
    (3, 8, '2026-11-13 19:20:00-03', '2026-11-13 20:16:00-03'),
    (3, 9, '2026-11-13 19:20:00-03', '2026-11-13 20:17:00-03'),
    (3, 10, '2026-11-13 19:20:00-03', '2026-11-13 20:18:00-03'),
    (3, 11, '2026-11-13 19:20:00-03', '2026-11-13 20:19:00-03'),
    (3, 12, '2026-11-13 19:20:00-03', '2026-11-13 20:20:00-03'),
    (3, 13, '2026-11-13 19:20:00-03', '2026-11-13 20:21:00-03'),
    (3, 14, '2026-11-13 19:20:00-03', '2026-11-13 20:22:00-03'),
    (3, 15, '2026-11-13 19:20:00-03', '2026-11-13 20:23:00-03'),
    (3, 16, '2026-11-13 19:20:00-03', '2026-11-13 20:24:00-03'),
    (3, 17, '2026-11-13 19:20:00-03', '2026-11-13 20:25:00-03'),
    (3, 18, NULL, NULL),
    (3, 19, NULL, NULL),
    (3, 20, NULL, NULL),
    (3, 21, NULL, NULL),
    (3, 22, NULL, NULL),
    (4, 3, '2026-11-20 19:20:00-03', '2026-11-20 20:11:00-03'),
    (4, 4, '2026-11-20 19:20:00-03', '2026-11-20 20:12:00-03'),
    (4, 5, '2026-11-20 19:20:00-03', '2026-11-20 20:13:00-03'),
    (4, 6, '2026-11-20 19:20:00-03', '2026-11-20 20:14:00-03'),
    (4, 7, '2026-11-20 19:20:00-03', '2026-11-20 20:15:00-03'),
    (4, 8, '2026-11-20 19:20:00-03', '2026-11-20 20:16:00-03'),
    (4, 9, '2026-11-20 19:20:00-03', '2026-11-20 20:17:00-03'),
    (4, 10, '2026-11-20 19:20:00-03', '2026-11-20 20:18:00-03'),
    (4, 11, '2026-11-20 19:20:00-03', '2026-11-20 20:19:00-03'),
    (4, 12, '2026-11-20 19:20:00-03', '2026-11-20 20:20:00-03'),
    (4, 13, '2026-11-20 19:20:00-03', '2026-11-20 20:21:00-03'),
    (4, 14, '2026-11-20 19:20:00-03', '2026-11-20 20:22:00-03'),
    (4, 15, '2026-11-20 19:20:00-03', '2026-11-20 20:23:00-03'),
    (4, 16, '2026-11-20 19:20:00-03', '2026-11-20 20:24:00-03'),
    (4, 17, '2026-11-20 19:20:00-03', '2026-11-20 20:25:00-03'),
    (4, 18, '2026-11-20 19:20:00-03', '2026-11-20 20:26:00-03'),
    (4, 19, '2026-11-20 19:20:00-03', '2026-11-20 20:27:00-03'),
    (4, 20, '2026-11-20 19:20:00-03', NULL),
    (4, 21, NULL, NULL),
    (4, 22, NULL, NULL),
    (5, 3, NULL, NULL),
    (5, 4, NULL, NULL),
    (5, 5, NULL, NULL),
    (5, 6, NULL, NULL),
    (5, 7, NULL, NULL),
    (5, 8, NULL, NULL),
    (5, 9, NULL, NULL),
    (5, 10, NULL, NULL),
    (5, 11, NULL, NULL),
    (5, 12, NULL, NULL),
    (5, 13, NULL, NULL),
    (5, 14, NULL, NULL),
    (5, 15, NULL, NULL),
    (5, 16, NULL, NULL),
    (5, 17, NULL, NULL),
    (5, 18, NULL, NULL),
    (5, 19, NULL, NULL),
    (5, 20, NULL, NULL),
    (5, 21, NULL, NULL),
    (5, 22, NULL, NULL);

INSERT INTO respostas (participacao_id, avaliacao_questao_id, alternativa_id, respondida_em)
SELECT p.id, aq.id, escolhida.id, p.iniciada_em + INTERVAL '15 minutes'
FROM participacoes p
JOIN avaliacao_questoes aq ON aq.avaliacao_id = p.avaliacao_id
JOIN questoes q ON q.id = aq.questao_id
JOIN tipos_questao tq ON tq.id = q.tipo_questao_id
CROSS JOIN LATERAL (
    SELECT a.id FROM alternativas a
    WHERE a.questao_id = q.id
      AND a.correta = (
          mod((p.aluno_id - 2) * 3 + p.avaliacao_id * 2 + aq.ordem, 10)
          < CASE p.avaliacao_id WHEN 1 THEN 4 WHEN 2 THEN 6 WHEN 3 THEN 5 ELSE 8 END
      )
    ORDER BY a.ordem
    FETCH FIRST 1 ROW ONLY
) escolhida
WHERE tq.codigo = 'MULTIPLA_ESCOLHA'
  AND p.iniciada_em IS NOT NULL
  AND (p.entregue_em IS NOT NULL OR aq.ordem <= 3)
  AND mod(p.aluno_id + aq.ordem + p.avaliacao_id, 13) <> 0;

INSERT INTO respostas (participacao_id, avaliacao_questao_id, texto_resposta, nota_obtida, respondida_em)
SELECT p.id, aq.id,
       CASE WHEN mod(p.aluno_id + q.id + p.avaliacao_id, 5) < 2
            THEN 'Ainda nao consegui explicar esse conceito com um exemplo.'
            ELSE CASE q.id

                WHEN 21 THEN 'A chave primaria identifica cada linha; a estrangeira referencia uma chave e garante integridade referencial.'

                WHEN 22 THEN 'WHERE filtra linhas antes do agrupamento; HAVING filtra os grupos produzidos pela agregacao.'

                WHEN 23 THEN 'A 3FN elimina dependencias transitivas de atributos nao chave em relacao as chaves candidatas.'

                WHEN 24 THEN 'Para listar todos os alunos, inclusive os que nao responderam, uso LEFT JOIN entre participacoes e respostas.'

                WHEN 25 THEN 'O debito e o credito devem ser atomicos: ambos confirmam ou ambos sao desfeitos em caso de falha.'

                WHEN 26 THEN 'DELETE remove linhas e preserva a estrutura; DROP TABLE remove a propria tabela.'

                WHEN 27 THEN 'Uma tabela associativa contem chaves estrangeiras das duas entidades e uma restricao de unicidade do par.'

                WHEN 28 THEN 'Comparacoes comuns com NULL produzem desconhecido; para testar ausencia de valor uso IS NULL.'

                WHEN 29 THEN 'UNIQUE impede valores repetidos na chave escolhida e permite que a regra seja garantida pelo banco.'

                WHEN 30 THEN 'Um indice pode acelerar filtros e juncoes seletivos, mas ocupa espaco e aumenta o custo de escrita.'

            END
       END,
       CASE WHEN p.avaliacao_id = 4 AND mod(p.aluno_id, 4) = 0 THEN NULL
            ELSE round(aq.valor * mod(p.aluno_id + q.id + p.avaliacao_id, 5) / 4, 2)
       END,
       p.iniciada_em + INTERVAL '25 minutes'
FROM participacoes p
JOIN avaliacao_questoes aq ON aq.avaliacao_id = p.avaliacao_id
JOIN questoes q ON q.id = aq.questao_id
JOIN tipos_questao tq ON tq.id = q.tipo_questao_id
WHERE tq.codigo = 'TEXTO_LIVRE'
  AND p.entregue_em IS NOT NULL
  AND mod(p.aluno_id + aq.ordem + p.avaliacao_id, 13) <> 0;

COMMIT;
